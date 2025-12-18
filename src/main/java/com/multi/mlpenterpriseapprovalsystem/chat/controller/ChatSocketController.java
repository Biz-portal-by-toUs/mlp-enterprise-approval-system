package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.MessageType;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.ChatRedisPublisher;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.RedisChatMessage;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatMessageRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * 채팅 웹소켓 컨트롤러 (레디스 Pub/sub)
 *
 * @author : 김승기
 * @filename : WebSocketController
 * @since : 2025. 12. 17. 수요일
 */

@Controller
@RequiredArgsConstructor
public class ChatSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
     private final ChatRoomMemberRepository chatRoomMemberRepository;
     private final ChatRedisPublisher chatRedisPublisher;


    @MessageMapping("/chat/send")
    public void send(ResChatMessageDto request) {

        System.out.println("🔥 CONTROLLER RECEIVED: " + request.getContent());

        ChatMessage saved = chatMessageRepository.save(
                ChatMessage.builder()
                        .roomNo(request.getRoomNo())
                        .senderId(1L)
                        .content(request.getContent())
                        .type(MessageType.TEXT)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        RedisChatMessage redisMessage = new RedisChatMessage(
                saved.getRoomNo(),
                saved.getSenderId(),
                saved.getContent(),
                saved.getType(),
                saved.getCreatedAt()
        );

        chatRedisPublisher.publish(redisMessage);
    }

}
