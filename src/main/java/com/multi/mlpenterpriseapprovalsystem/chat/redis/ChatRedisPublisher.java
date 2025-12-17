package com.multi.mlpenterpriseapprovalsystem.chat.redis;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ChatRedisPublisher
 * @since : 2025. 12. 17. 수요일
 */
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(RedisChatMessage message) {
        System.out.println(message);
        redisTemplate.convertAndSend(
                "chat:room:" + message.getRoomNo(),
                message
        );
    }
}