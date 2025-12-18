package com.multi.mlpenterpriseapprovalsystem.chat.service;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatRoom;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.MessageType;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatMessageSendDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatMessageRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomMemberRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ChatMessageService
 * @since : 2025. 12. 18. 목요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final EmployeeRepository employeeRepository;

    public ResChatMessageDto sendMessage(ReqChatMessageSendDto request) {

        // TODO: SecurityContext로 교체
        String myEmpId = "EMP0001";

        // 1️⃣ 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomNo())
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 채팅방입니다.")
                );

        // 2️⃣ 채팅방 멤버 검증 (중요)
        boolean isMember = chatRoomMemberRepository
                .existsByChatRoom_RoomNoAndEmployee_EmpId(
                        request.getRoomNo(),
                        myEmpId
                );

        if (!isMember) {
            throw new IllegalArgumentException("채팅방에 메시지를 보낼 권한이 없습니다.");
        }

        // 3️⃣ 발신자 정보 조회
        Employee sender = employeeRepository.findByEmpId(myEmpId)
                .orElseThrow(() ->
                        new IllegalArgumentException("사원이 존재하지 않습니다.")
                );

        // 4️⃣ MongoDB 메시지 저장
        ChatMessage message = ChatMessage.builder()
                .roomNo(request.getRoomNo())
                .senderEmpId(sender.getEmpId())
                .senderName(sender.getEmpName())
                .content(request.getContent())
                .type(request.getType() == null ? MessageType.TEXT : request.getType())
                .createdAt(LocalDateTime.now())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // 5️⃣ ChatRoom 마지막 메시지 갱신 (MySQL)
        chatRoom.updateLastMessage(
                savedMessage.getContent(),
                savedMessage.getCreatedAt()
        );

        return ResChatMessageDto.from(savedMessage);
    }

    /**
     * 채팅 메시지 조회 (무한 스크롤)
     */
    public List<ResChatMessageDto> getMessages(
            Long roomNo,
            LocalDateTime cursor,
            int size
    ) {

        List<?> messages;

        if (cursor == null) {
            // 최초 진입
            messages = chatMessageRepository
                    .findByRoomNoOrderByCreatedAtDesc(roomNo)
                    .stream()
                    .limit(size)
                    .toList();
        } else {
            // 무한 스크롤
            messages = chatMessageRepository
                    .findByRoomNoAndCreatedAtLessThanOrderByCreatedAtDesc(
                            roomNo,
                            cursor
                    )
                    .stream()
                    .limit(size)
                    .toList();
        }

        return messages.stream()
                .map(m -> ResChatMessageDto.from((com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage) m))
                .collect(Collectors.toList());
    }
}