package com.multi.mlpenterpriseapprovalsystem.chat.service;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.MessageType;
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

        boolean isMember = chatRoomMemberRepository
                .existsByChatRoom_RoomNoAndEmployee_EmpId(request.getRoomNo(), empId);

        if (!isMember) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        Employee sender = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        ChatMessage message = ChatMessage.builder()
                .roomNo(request.getRoomNo())
                .senderEmpId(sender.getEmpId())
                .senderName(sender.getEmpName())
                .content(request.getContent())
                .type(request.getType() == null ? MessageType.TEXT : request.getType())
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        ResChatMessageDto responseDto = ResChatMessageDto.from(savedMessage);

        redisPublisher.publish(request.getRoomNo(), responseDto);

        chatRoom.updateLastMessage(savedMessage.getContent(), savedMessage.getCreatedAt());

        chatRoomMemberRepository.increaseUnreadForOthers(request.getRoomNo(), empId);
        List<String> memberEmpIds = chatRoomMemberRepository.findEmpIdsByRoomNo(request.getRoomNo());

        for (String targetEmpId : memberEmpIds) {
            int unread = targetEmpId.equals(empId)
                    ? 0
                    : chatRoomMemberRepository.findUnreadCount(request.getRoomNo(), targetEmpId);

            ResChatRoomUpdateDto updateDto = new ResChatRoomUpdateDto(
                    request.getRoomNo(),
                    savedMessage.getContent(),
                    savedMessage.getCreatedAt().toString(),
                    unread
            );

            redisPublisher.publishRoomUpdate(targetEmpId, updateDto);
        }

        return responseDto;
    }

    /**
     * 채팅 메시지 조회 (무한 스크롤)
     */
    @Transactional(readOnly = true)
    public List<ResChatMessageDto> getMessages(
            Long roomNo,
            LocalDateTime cursor,
            int size,
            String empId
    ) {
        boolean isMember = chatRoomMemberRepository
                .existsByChatRoom_RoomNoAndEmployee_EmpId(roomNo, empId);

        if (!isMember) {
            throw new CustomException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        List<ChatMessage> messages;

        if (cursor == null) {
            messages = chatMessageRepository
                    .findByRoomNoOrderByCreatedAtDesc(roomNo)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
        } else {
            messages = chatMessageRepository
                    .findByRoomNoAndCreatedAtLessThanOrderByCreatedAtDesc(roomNo, cursor)
                    .stream()
                    .limit(size)
                    .collect(Collectors.toList());
        }

        return messages.stream()
                .map(ResChatMessageDto::from)
                .collect(Collectors.toList());
    }
}