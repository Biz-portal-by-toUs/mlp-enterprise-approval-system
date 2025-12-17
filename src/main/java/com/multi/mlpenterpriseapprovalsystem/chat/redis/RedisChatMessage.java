package com.multi.mlpenterpriseapprovalsystem.chat.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : RedisChatMessage
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedisChatMessage {

    private Long roomNo;
    private Long senderId;
    private String content;
    private MessageType type;
    private LocalDateTime createdAt;


}