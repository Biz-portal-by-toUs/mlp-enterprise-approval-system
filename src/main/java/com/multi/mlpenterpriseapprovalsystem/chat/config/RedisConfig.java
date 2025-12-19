package com.multi.mlpenterpriseapprovalsystem.chat.config;

import com.multi.mlpenterpriseapprovalsystem.chat.redis.ChatRedisSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis 연결 및 청취자 설정
 * Pub/Sub 메시지를 처리하기 위한 리스너 컨테이너 및 데이터 저장/전송을 위한 RedisTemplate을 빈(Bean)으로 등록
 * @author : 김승기
 * @filename : RedisConfig
 * @since : 2025. 12. 17. 수요일
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ChatRedisSubscriber redisSubscriber;

//    @Bean
//    public ObjectMapper redisObjectMapper() {
//        ObjectMapper om = new ObjectMapper();
//        om.registerModule(new JavaTimeModule());
//        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        return om;
//    }

//    @Bean
//    public RedisTemplate<String, String> redisTemplate() {
//        RedisTemplate<String, String> t = new RedisTemplate<>();
//        t.setConnectionFactory(redisConnectionFactory);
//        t.setKeySerializer(new StringRedisSerializer());
//        t.setValueSerializer(new StringRedisSerializer());
//        return t;
//    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        log.info("[REDIS LISTENER] subscribe pattern=room:*");
        c.setConnectionFactory(redisConnectionFactory);

        c.addMessageListener(redisSubscriber, new PatternTopic("room:*"));
        c.addMessageListener(redisSubscriber, new PatternTopic("user:*:room-update"));
        return c;
    }
}