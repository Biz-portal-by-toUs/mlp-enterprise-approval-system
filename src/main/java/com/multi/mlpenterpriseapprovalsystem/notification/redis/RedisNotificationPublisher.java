package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.notification.dto.NewLoginRedisMessage;
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
public class RedisNotificationPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic notificationTopic;
    private final ObjectMapper objectMapper;
    private final ChannelTopic loginDetectTopic;


    public RedisNotificationPublisher(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("notificationTopic") ChannelTopic notificationTopic,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper, // 주입받을 빈 이름 지정
            @Qualifier("loginDetectTopic") ChannelTopic loginDetectTopic
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.notificationTopic = notificationTopic;
        this.objectMapper = objectMapper;
        this.loginDetectTopic = loginDetectTopic;
    }

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

    public void publishNewLogin(String empId, String deviceId, String ip) {
        NewLoginRedisMessage msg = new NewLoginRedisMessage(
                empId, deviceId, ip, java.time.Instant.now().toString()
        );
        try {
            String payload = objectMapper.writeValueAsString(msg);
            stringRedisTemplate.convertAndSend(loginDetectTopic.getTopic(), payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize NewLoginRedisMessage", e);
        }
    }


}