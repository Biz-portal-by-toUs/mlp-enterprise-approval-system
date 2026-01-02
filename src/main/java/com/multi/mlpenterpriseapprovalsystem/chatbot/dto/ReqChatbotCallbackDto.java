package com.multi.mlpenterpriseapprovalsystem.chatbot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String messageId;      // ✅ assistantMessageId (Mongo _id String)
    private String chunk;          // 스트리밍 delta
    private Boolean done;          // 완료 여부
    private Boolean success;       // 성공/실패
    private String errorMessage;   // 실패 시 메시지
}