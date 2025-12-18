package com.multi.mlpenterpriseapprovalsystem.chat.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis에서 발행된 메시지를 수신하여 클라이언트에게 전달
 *
 * @author : 김승기
 * @filename : ChatRedisSubscriber
 * @since : 2025. 12. 17. 수요일
 */
@Component
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {

        System.out.println("🔥 REDIS SUBSCRIBE RECEIVED");
        try {
            RedisChatMessage chatMessage =
                    objectMapper.readValue(message.getBody(), RedisChatMessage.class);

            messagingTemplate.convertAndSend(
                    "/sub/chat/room/" + chatMessage.getRoomNo(),
                    chatMessage
            );
            System.out.println(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}