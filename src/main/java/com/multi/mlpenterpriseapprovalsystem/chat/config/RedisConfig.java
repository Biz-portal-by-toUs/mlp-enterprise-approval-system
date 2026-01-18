package com.multi.mlpenterpriseapprovalsystem.chat.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.ChatRedisSubscriber;
import com.multi.mlpenterpriseapprovalsystem.notification.redis.ChatUnreadSubscriber;
import com.multi.mlpenterpriseapprovalsystem.notification.redis.RedisLoginDetectSubscriber;
import com.multi.mlpenterpriseapprovalsystem.notification.redis.RedisNotificationSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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

    private final RedisNotificationSubscriber redisNotificationSubscriber;
    private final ChatUnreadSubscriber chatUnreadSubscriber;
    private final RedisLoginDetectSubscriber redisLoginDetectSubscriber; // ✅ 추가 주입

    // ==========================================
    // 1. 모든 토픽(Topic) 정의 통합
    // ==========================================
    @Bean(name = "notificationTopic")
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("notifications:pubsub"); // 기존 이름 유지
    }

    @Bean(name = "loginDetectTopic")
    public ChannelTopic loginDetectTopic() {
        return new ChannelTopic("login-detect:pubsub"); // 기존 이름 유지
    }

    @Bean(name = "chatUnreadTopic")
    public ChannelTopic chatUnreadTopic() {
        return new ChannelTopic("chat-unread-count");
    }



    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> t = new RedisTemplate<>();
        t.setConnectionFactory(redisConnectionFactory);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new StringRedisSerializer());
        return t;
    }

    /**
     * 챗봇 메시지(객체) 저장용 템플릿
     */
    /**
     * 챗봇 메시지 객체 저장용 템플릿 (JSON 직렬화 최신 방식)
     */
    @Bean(name = "chatbotRedisTemplate")
    public RedisTemplate<String, Object> chatbotRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. ObjectMapper 설정 (LocalDateTime 대응)
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 다형성(Polymorphism)을 위한 타입 정보 저장 설정
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 2. RedisSerializer 인터페이스 직접 구현 (익명 클래스 방식)
        RedisSerializer<Object> serializer = new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object value) {
                if (value == null) return new byte[0];
                try {
                    return mapper.writeValueAsBytes(value);
                } catch (Exception e) {
                    throw new SerializationException("Redis 직렬화 에러: " + e.getMessage(), e);
                }
            }

            @Override
            public Object deserialize(byte[] bytes) throws SerializationException {
                if (bytes == null || bytes.length == 0) return null;
                try {
                    return mapper.readValue(bytes, Object.class);
                } catch (Exception e) {
                    throw new SerializationException("Redis 역직렬화 에러: " + e.getMessage(), e);
                }
            }
        };

        // 3. 직렬화 적용
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(RedisSerializer.string());
        template.setHashValueSerializer(serializer);

        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        log.info("[REDIS LISTENER] subscribe pattern=room:*");
        c.setConnectionFactory(redisConnectionFactory);

        c.addMessageListener(redisSubscriber, new PatternTopic("room:*"));
        c.addMessageListener(redisSubscriber, new PatternTopic("user:*:room-update"));

        c.addMessageListener(redisSubscriber, new ChannelTopic("user:status-update"));

        c.addMessageListener(redisNotificationSubscriber, notificationTopic());

        c.addMessageListener(redisNotificationSubscriber, loginDetectTopic());

        c.addMessageListener(chatUnreadSubscriber, chatUnreadTopic());
        return c;
    }
}