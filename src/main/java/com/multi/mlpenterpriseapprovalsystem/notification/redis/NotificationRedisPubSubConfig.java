package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * 알림함 redis config
 *
 * @author : 김승기
 * @filename : NotificationRedisPubSubConfig
 * @since : 2026. 1. 6. 화요일
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotificationRedisPubSubConfig {

    @Bean(name = "notificationTopic")
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("notifications:pubsub");
    }

    // ✅로그인 감지 토픽
    @Bean(name = "loginDetectTopic")
    public ChannelTopic loginDetectTopic() {
        return new ChannelTopic("login-detect:pubsub");
    }

    @Bean(name = "notificationRedisMessageListenerContainer")
    public RedisMessageListenerContainer notificationRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisNotificationSubscriber subscriber,
            @Qualifier("notificationTopic") ChannelTopic notificationTopic
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(subscriber, notificationTopic);

        return container;
    }

    // ✅ 추가: 로그인 감지 컨테이너
    @Bean(name = "loginDetectRedisMessageListenerContainer")
    public RedisMessageListenerContainer loginDetectRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisLoginDetectSubscriber subscriber,
            @Qualifier("loginDetectTopic") ChannelTopic loginDetectTopic
    ) {
        log.info("[LOGIN-DETECT] subscribe topic={}", loginDetectTopic.getTopic());
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, loginDetectTopic);
        return container;
    }
}