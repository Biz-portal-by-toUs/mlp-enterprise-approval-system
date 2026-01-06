package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

/**
 * 알림함 publisher
 *
 * @author : 김승기
 * @filename : RedisNotificationPublisher
 * @since : 2026. 1. 6. 화요일
 */
@Service
@RequiredArgsConstructor
public class RedisNotificationPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("notificationTopic")
    private final ChannelTopic notificationTopic;
    private final ObjectMapper objectMapper;

    public void publish(String empId, Long notiNo) {
        NotificationRedisMessage msg = new NotificationRedisMessage(empId, notiNo);

        final String payload;
        try {
            payload = objectMapper.writeValueAsString(msg);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize NotificationRedisMessage", e);
        }

        stringRedisTemplate.convertAndSend(notificationTopic.getTopic(), payload);
    }
}