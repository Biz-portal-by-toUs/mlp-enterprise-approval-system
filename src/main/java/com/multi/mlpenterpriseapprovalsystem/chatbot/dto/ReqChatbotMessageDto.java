package com.multi.mlpenterpriseapprovalsystem.chatbot.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * fastAPI에 보낼 질문 dto
 *
 * @author : 김승기
 * @filename : ReqChatbotMessageDto
 * @since : 2026. 1. 1. 목요일
 */
@Getter
@NoArgsConstructor
public class ReqChatbotMessageDto {
    private String question;      // 사용자가 입력한 질문
}