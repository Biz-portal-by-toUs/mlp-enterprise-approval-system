package com.multi.mlpenterpriseapprovalsystem.chatbot.domain;

/**
 * 에러 이벤트 클래스
 *
 * @author : 김승기
 * @filename : ChatbotRequestFailedEvent
 * @since : 2026. 1. 12. 월요일
 */
public record ChatbotRequestFailedEvent(String messageId, String errorMessage) {}