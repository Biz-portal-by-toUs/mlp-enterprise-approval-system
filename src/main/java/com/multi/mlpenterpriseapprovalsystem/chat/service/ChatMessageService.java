package com.multi.mlpenterpriseapprovalsystem.chat.service;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.*;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatMessageSendDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomUpdateDto;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.ChatRedisPublisher;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatMessageRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomMemberRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 채팅 메세지 서비스 (메세지 발신, 조회)
 *
 * @author : 김승기
 * @filename : ChatMessageService
 * @since : 2025. 12. 18. 목요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final EmployeeRepository employeeRepository;
    private final ChatRedisPublisher redisPublisher;
    private final StringRedisTemplate redisTemplate;

    /**
     * 메시지 전송
     */
    public ResChatMessageDto sendMessage(ReqChatMessageSendDto request, String empId) {

        ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomNo())
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatRoomMember senderMember = chatRoomMemberRepository
                .findByChatRoom_RoomNoAndEmployee_EmpId(request.getRoomNo(), empId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        LocalDateTime now = LocalDateTime.now();

        if (!senderMember.isActive()) {
            if (chatRoom.getRoomType() == RoomType.ONE) {
                chatRoomMemberRepository.reactivateMe(request.getRoomNo(), empId, now);
            } else {
                throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
            }
        }

        Employee sender = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        ChatMessage message = ChatMessage.builder()
                .roomNo(request.getRoomNo())
                .senderEmpId(sender.getEmpId())
                .senderName(sender.getEmpName())
                .content(request.getContent())
                .type(request.getType() == null ? MessageType.TEXT : request.getType())
                .createdAt(now)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 1:1이면 상대가 나가(active=false) 있어도 메시지 한 번 오면 다시 뜨게(자동 복귀)
        //  joinedAt을 now로 바꿔서 "복귀 이후 메시지만" 보이게 한다.
        if (chatRoom.getRoomType() == RoomType.ONE) {
            chatRoomMemberRepository.reactivateOthersForOneToOne(request.getRoomNo(), empId, now);
        }

        ResChatMessageDto responseDto = ResChatMessageDto.from(savedMessage);
        redisPublisher.publish(request.getRoomNo(), responseDto);

        chatRoom.updateLastMessage(savedMessage.getContent(), savedMessage.getCreatedAt());

        String viewingKey = "chat:room:" + request.getRoomNo() + ":viewing";
        Set<String> viewingEmpIds = redisTemplate.opsForSet().members(viewingKey);
        if (viewingEmpIds == null) viewingEmpIds = Set.of();

        chatRoomMemberRepository.increaseUnreadExceptViewers(request.getRoomNo(), empId, viewingEmpIds);

        String preview = chatRoom.getLastMessage();                 // ← trim 적용된 값
        String previewAtIso = chatRoom.getLastMessageAt().toString(); // ← updateLastMessage에서 세팅된 값

        List<String> memberEmpIds = chatRoomMemberRepository.findEmpIdsByRoomNo(request.getRoomNo());

        for (String targetEmpId : memberEmpIds) {

            long unreadLong;
            if (targetEmpId.equals(empId)) {
                unreadLong = 0L;
            } else {
                Long v = chatRoomMemberRepository.findUnreadCount(request.getRoomNo(), targetEmpId);
                unreadLong = (v == null ? 0L : v);
            }

            int unread = unreadLong > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) unreadLong;

            // ✅ [수정 포인트] 각 수신자(targetEmpId)의 시점에서 보여줄 방 이름 결정
            String finalRoomName;
            if (chatRoom.getRoomType() == RoomType.ONE) {
                // 1:1 채팅방: 수신자(targetEmpId)가 아닌 '상대방'의 이름을 찾음
                finalRoomName = chatRoom.getMembers().stream()
                        .filter(m -> !m.getEmployee().getEmpId().equals(targetEmpId))
                        .map(m -> m.getEmployee().getEmpName())
                        .findFirst()
                        .orElse("알 수 없는 사용자");
            } else {
                // 그룹 채팅방: 저장된 방 이름 사용 (없으면 기본값)
                finalRoomName = chatRoom.getRoomName() != null ? chatRoom.getRoomName() : "그룹 채팅";
            }

            ResChatRoomUpdateDto updateDto = new ResChatRoomUpdateDto(
                    request.getRoomNo(),
                    preview,
                    previewAtIso,
                    unread,
                    finalRoomName // ✅ 계산된 실시간 방 이름을 전달
            );

            redisPublisher.publishRoomUpdate(targetEmpId, updateDto);
        }

        return responseDto;
    }

    /**
     * 채팅 메시지 조회 (무한 스크롤)
     * - active=true 멤버만 접근 가능
     * - joinedAt 이후 메시지만 보여준다
     */
    @Transactional(readOnly = true)
    public List<ResChatMessageDto> getMessages(
            Long roomNo,
            LocalDateTime cursor,
            int size,
            String empId
    ) {
        ChatRoomMember member = chatRoomMemberRepository
                .findByChatRoom_RoomNoAndEmployee_EmpIdAndIsActiveTrue(roomNo, empId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        LocalDateTime joinedAt = member.getJoinedAt();

        List<ChatMessage> messages;
        if (cursor == null) {
            messages = chatMessageRepository
                    .findByRoomNoAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(roomNo, joinedAt)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
        } else {
            messages = chatMessageRepository
                    .findByRoomNoAndCreatedAtLessThanAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            roomNo, cursor, joinedAt
                    )
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
        }

        return messages.stream()
                .map(ResChatMessageDto::from)
                .collect(Collectors.toList());
    }
}