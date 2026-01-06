package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import lombok.RequiredArgsConstructor;
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
public class NotificationRedisPubSubConfig {

    @Bean(name = "notificationTopic")
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("notifications:pubsub");
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
}