package com.multi.mlpenterpriseapprovalsystem.chat.redis;

/**
 * 메세지 발신 (서버가 받은 메세지를 redis에 뿌림)
 * @author : 김승기
 * @filename : ChatRedisPublisher
 * @since : 2025. 12. 17. 수요일
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.MessageType;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatRoomUpdateDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class ChatRedisPublisher {
    private final RedisTemplate<String, String> redisTemplate;

//    @Qualifier("redisObjectMapper")
    private final ObjectMapper objectMapper;
    private MessageType messageType;

    public ChatRedisPublisher(
            @Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(Long roomNo, ResChatMessageDto dto) {
        String channel = "room:" + roomNo;
        try {
            String payload = objectMapper.writeValueAsString(dto);
            log.info("[REDIS PUB] channel={}, payloadLen={}, dto={}", channel, payload.length(), dto);
            redisTemplate.convertAndSend(channel, payload);
            log.info("[REDIS PUB] published OK");
        } catch (JsonProcessingException e) {
            log.error("[REDIS PUB] JSON serialization failed. dto={}", dto, e);
            throw new CustomException(ErrorCode.JSON_PARSING_ERROR);
        } catch (Exception e) {
            log.error("[REDIS PUB] Redis publish failed. channel={}", channel, e);
            throw new CustomException(ErrorCode.REDIS_PUB_SUB_ERROR);
        }
    }

    public void publishRoomUpdate(String empId, ResChatRoomUpdateDto dto) {
        try {
            String channel = "user:" + empId + ":room-update";
            String payload = objectMapper.writeValueAsString(dto);
            redisTemplate.convertAndSend(channel, payload);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.JSON_PARSING_ERROR);
        } catch (Exception e) {
            log.error("[ROOM-UPDATE PUB] failed empId={}", empId, e);
            throw new CustomException(ErrorCode.REDIS_PUB_SUB_ERROR);
        }
    }

    public void publishSystem(Long roomNo, String content) {
        try {
            ResChatMessageDto dto = new ResChatMessageDto(
                    null,               // messageId (저장 안 하니 null)
                    roomNo,
                    "SYSTEM",           // senderEmpId 같은 거 필요하면 "SYSTEM"
                    "SYSTEM",         // 화면에 "SYSTEM" 또는 "알림"
                    content,
                    MessageType.SYSTEM,           // type
                    LocalDateTime.now().toString()
            );
            publish(roomNo, dto); // 기존 room:{roomNo} 채널로 발행
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}