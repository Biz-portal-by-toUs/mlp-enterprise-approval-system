package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.common.sse.SseManager;
import com.multi.mlpenterpriseapprovalsystem.notification.dto.NewLoginRedisMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 새로그인 감지 알림
 *
 * @author : 권지영
 * @filename : RedisLoginDetectSubscriber
 * @since : 2026. 1. 10. 토요일
 */
@Component
@Slf4j
public class RedisLoginDetectSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final SseManager sseManager;

    public RedisLoginDetectSubscriber(
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            SseManager sseManager
    ) {
        this.objectMapper = objectMapper;
        this.sseManager = sseManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String raw = new String(message.getBody(), StandardCharsets.UTF_8);

        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);

        log.warn("[LOGIN-DETECT SUB] channel={}, pattern={}, raw={}",
                channel,
                pattern == null ? null : new String(pattern, StandardCharsets.UTF_8),
                raw);


        final NewLoginRedisMessage msg;
        try {
            msg = objectMapper.readValue(raw, NewLoginRedisMessage.class);
        } catch (Exception e) {
            log.warn("[LOGIN-DETECT SUB] parse failed: {}", e.toString());
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "같은 계정으로 새 로그인이 감지되었습니다.");
        payload.put("ip", msg.getIp());
        payload.put("at", msg.getAt());

        log.warn("[LOGIN-DETECT SUB] send empId={}, excludeDeviceId={}, event=new-login",
                msg.getEmpId(), msg.getDeviceId());

        // ✅ 새 로그인 기기 제외, 나머지 기존 기기들만
        sseManager.sendToOtherDevices(msg.getEmpId(), msg.getDeviceId(), "new-login", payload);
    }
}
