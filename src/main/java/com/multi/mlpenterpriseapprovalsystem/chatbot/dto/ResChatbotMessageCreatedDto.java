package com.multi.mlpenterpriseapprovalsystem.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 챗봇 메세지 생성 ResDto
 *
 * @author : 김승기
 * @filename : ResChatbotMessageCreatedDto
 * @since : 2026. 1. 1. 목요일
 */

@Getter
@AllArgsConstructor
public class ResChatbotMessageCreatedDto {
    private String assistantMessageId; // FastAPI 콜백이 이 id로 돌아오게 하면 편함
}