package com.multi.mlpenterpriseapprovalsystem.common.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
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


    public void requestChatbotAnswer(String assistantMessageId, String empId, String comId, String question) {

        Map<String, Object> body = new HashMap<>();
        body.put("messageId", assistantMessageId);
        body.put("empId", empId);
        body.put("comId", comId);
        body.put("question", question);


        body.put("callbackUrl", callbackUrl);
        body.put("callbackKey", callbackKey);

        fastApiWebClient.post()
                .uri("/ai/chatbot/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("[AI][CHATBOT] requested messageId={}, res={}", assistantMessageId, res))
                .doOnError(e -> log.error("[AI][CHATBOT] request failed messageId={}", assistantMessageId, e))
                .subscribe();
    }
}