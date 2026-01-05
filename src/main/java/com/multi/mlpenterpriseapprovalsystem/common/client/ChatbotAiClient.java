package com.multi.mlpenterpriseapprovalsystem.common.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 챗봇 질의 요청 클라이언트
 *
 * @author : 김승기
 * @filename : ChatbotAiClient
 * @since : 2026. 1. 1. 목요일
 */
@Component
@Slf4j
public class ChatbotAiClient {

    private final WebClient fastApiWebClient;

    public ChatbotAiClient(@Qualifier("fastApiWebClient") WebClient fastApiWebClient) {
        this.fastApiWebClient = fastApiWebClient;
    }

    @Value("${internal.ai.chatbot.callback-url}")
    private String callbackUrl;

    @Value("${internal.ai.callback-key}")
    private String callbackKey;


    /**
     * AI에게 질문 및 대화 문맥 전달
     *
     * @param assistantMessageId 답변을 매칭할 고유 ID (empId_sessionId_timestamp)
     * @param empId             사번
     * @param comId             회사 ID
     * @param question          현재 질문
     * @param history           Redis에서 조회한 이전 대화 내역 리스트
     */
    public void requestChatbotAnswer(
            String assistantMessageId,
            String empId,
            String comId,
            String question,
            List<Map<String, String>> history
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("messageId", assistantMessageId);
        body.put("empId", empId);
        body.put("comId", comId);
        body.put("question", question);

        body.put("history", history);

        body.put("callbackUrl", callbackUrl);
        body.put("callbackKey", callbackKey);

        log.info("[AI][CHATBOT] Requesting answer with context. messageId={}, historySize={}", assistantMessageId, history.size());

        // 2. 비동기 POST 요청 실행
        fastApiWebClient.post()
                .uri("/ai/chatbot/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("[AI][CHATBOT] Request success. res={}", res))
                .doOnError(e -> log.error("[AI][CHATBOT] Request failed. messageId={}, error={}", assistantMessageId, e.getMessage()))
                .subscribe(); // 비동기 실행
    }
}