package com.multi.mlpenterpriseapprovalsystem.chat.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Redis에서 발행된 메시지를 수신하여 클라이언트에게 전달
 *
 * @author : 김승기
 * @filename : ChatRedisSubscriber
 * @since : 2025. 12. 17. 수요일
 */
@Component
@Slf4j
public class ChatRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

//    @Qualifier("redisObjectMapper")
    private final ObjectMapper objectMapper;

    public ChatRedisSubscriber(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper
    ) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            // 방 메시지 브로드캐스트
            if (channel.startsWith("room:")) {
                ResChatMessageDto dto = objectMapper.readValue(body, ResChatMessageDto.class);
                String roomNo = channel.split(":")[1];
                messagingTemplate.convertAndSend("/sub/chat/rooms/" + roomNo, dto);
                return;
            }

            // 유저별 방 목록 업데이트
            if (channel.startsWith("user:") && channel.endsWith(":room-update")) {

                ResChatRoomUpdateDto dto = objectMapper.readValue(body, ResChatRoomUpdateDto.class);
                String empId = channel.split(":")[1];

                log.info("[ROOM-UPDATE WS] toUser={} dest=/user/sub/chat/room-updates", empId);

                messagingTemplate.convertAndSendToUser(
                        empId,
                        "/sub/chat/room-updates",
                        dto
                );
                return;
            }

            log.warn("[REDIS SUB] unknown channel={}", channel);

        } catch (JsonProcessingException e) {
            log.error("[REDIS SUB] JSON parsing failed. body={}", body, e);
        } catch (Exception e) {
            log.error("[REDIS SUB] Unexpected error. channel={}", channel, e);
        }
    }
}