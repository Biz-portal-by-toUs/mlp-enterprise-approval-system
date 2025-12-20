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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

        chatRoomMemberRepository.increaseUnreadForOthers(request.getRoomNo(), empId);

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

            ResChatRoomUpdateDto updateDto = new ResChatRoomUpdateDto(
                    request.getRoomNo(),
                    savedMessage.getContent(),
                    savedMessage.getCreatedAt().toString(), // ISO
                    unread
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