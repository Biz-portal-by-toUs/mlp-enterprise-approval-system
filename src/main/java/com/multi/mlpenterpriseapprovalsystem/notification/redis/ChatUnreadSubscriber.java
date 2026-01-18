package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.common.sse.SseManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 채팅 안읽은 메세지 sub
 *
 * @author : 김승기
 * @filename : ChatUnreadSubscriber
 * @since : 2026. 1. 18. 일요일
 */
@Component
@Slf4j
public class ChatUnreadSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SseManager sseManager;

    public ChatUnreadSubscriber(
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            SseManager sseManager
    ) {
        this.objectMapper = objectMapper;
        this.sseManager = sseManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String raw = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatUnreadRedisMessage msg = objectMapper.readValue(raw, ChatUnreadRedisMessage.class);

            log.info("[CHAT UNREAD SUB] empId={}, count={}", msg.getEmpId(), msg.getTotalCount());

            // SSE를 통해 해당 사용자에게 "TOTAL_CHAT_UNREAD" 이벤트 전송
            sseManager.sendToUser(msg.getEmpId(), "TOTAL_CHAT_UNREAD", msg.getTotalCount());

        } catch (Exception e) {
            log.error("채팅 안 읽은 개수 Redis 메시지 파싱 에러", e);
        }
    }
}