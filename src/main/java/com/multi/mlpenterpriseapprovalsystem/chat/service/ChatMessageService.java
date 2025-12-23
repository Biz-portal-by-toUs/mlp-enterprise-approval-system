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
    @Transactional
    public ResChatMessageDto sendMessage(ReqChatMessageSendDto request, String empId) {

        ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomNo())
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatRoomMember senderMember = chatRoomMemberRepository
                .findByChatRoom_RoomNoAndEmployee_EmpId(request.getRoomNo(), empId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ACCESS_DENIED));

        LocalDateTime now = LocalDateTime.now();

        // 1. 발신자 활성화 처리 (1:1 복귀 로직)
        if (!senderMember.isActive()) {
            if (chatRoom.getRoomType() == RoomType.ONE) {
                chatRoomMemberRepository.reactivateMe(request.getRoomNo(), empId, now);
                senderMember.reactivateNow(now);
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

        // 2. 1:1인 경우 상대방도 자동으로 활성화(복귀) 처리
        if (chatRoom.getRoomType() == RoomType.ONE) {
            chatRoomMemberRepository.reactivateOthersForOneToOne(request.getRoomNo(), empId, now);
            // ✅ 상대방 메모리 객체 상태도 활성화로 업데이트하여 카운트에 반영
            chatRoom.getMembers().forEach(m -> {
                if (!m.getEmployee().getEmpId().equals(empId)) m.reactivateNow(now);
            });
        }

        // 3. 현재 활성 상태인 멤버 수 계산
        int activeMemberCount = (int) chatRoom.getMembers().stream()
                .filter(ChatRoomMember::isActive)
                .count();

        ResChatMessageDto responseDto = ResChatMessageDto.from(savedMessage);
        redisPublisher.publish(request.getRoomNo(), responseDto);

        chatRoom.updateLastMessage(savedMessage.getContent(), savedMessage.getCreatedAt());

        // 안읽은 메시지 처리
        String viewingKey = "chat:room:" + request.getRoomNo() + ":viewing";
        Set<String> viewingEmpIds = redisTemplate.opsForSet().members(viewingKey);
        if (viewingEmpIds == null) viewingEmpIds = Set.of();

        chatRoomMemberRepository.increaseUnreadExceptViewers(request.getRoomNo(), empId, viewingEmpIds);

        String preview = chatRoom.getLastMessage();
        String previewAtIso = chatRoom.getLastMessageAt().toString();

        // 모든 멤버 아이디 가져오기 (실시간 알림 대상)
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

            // 수신자 시점의 방 이름 결정
            String finalRoomName;
            if (chatRoom.getRoomType() == RoomType.ONE) {
                finalRoomName = chatRoom.getMembers().stream()
                        .filter(m -> !m.getEmployee().getEmpId().equals(targetEmpId))
                        .map(m -> m.getEmployee().getEmpName())
                        .findFirst()
                        .orElse("알 수 없는 사용자");
            } else {
                finalRoomName = chatRoom.getRoomName() != null ? chatRoom.getRoomName() : "그룹 채팅";
            }



            ResChatRoomUpdateDto updateDto = new ResChatRoomUpdateDto(
                    request.getRoomNo(),
                    preview,
                    previewAtIso,
                    unread,
                    finalRoomName,
                    false,
                    chatRoom.getRoomType(),
                    activeMemberCount
            );

            System.out.println("########### activeMemberCount "+ activeMemberCount);

            if (message.getType() == MessageType.SYSTEM) {
                continue;
            }

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