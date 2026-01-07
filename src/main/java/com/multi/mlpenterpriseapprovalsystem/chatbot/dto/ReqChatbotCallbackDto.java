package com.multi.mlpenterpriseapprovalsystem.chatbot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * fastapi에서 스프링으로 요청하는 Callback reqDto
 *
 * @author : 김승기
 * @filename : ReqChatbotCallbackDto
 * @since : 2026. 1. 1. 목요일
 */
@Getter
@NoArgsConstructor
public class ReqChatbotCallbackDto {
    private String messageId;
    private String chunk;
    private Boolean done;
    private Boolean success;
    private String errorMessage;
    private String sessionId;

    private String actionId;         // null 가능
    private Map<String, Object> params; // null 가능
}