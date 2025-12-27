package com.multi.mlpenterpriseapprovalsystem.common.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * FastAPI서버에 요청 보내는 서비스
 *
 * @author : 김승기
 * @filename : MeetingAiClient
 * @since : 2025. 12. 26. 금요일
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MeetingAiClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${ai.fastapi.base-url}")
    private String fastApiBaseUrl;

    @Value("${internal.ai.callback-url}") // FastAPI가 결과를 PATCH로 호출할 Spring 주소
    private String callbackUrl;

    @Value("${internal.ai.callback-key}")
    private String callbackKey;

    public void requestAi(Long meetNo, String objectKey) {

        WebClient wc = webClientBuilder.baseUrl(fastApiBaseUrl).build();

        Map<String, Object> body = Map.of(
                "meetNo", meetNo,
                "objectKey", objectKey,
                "callbackUrl", callbackUrl,      // 예: http://spring:8090/api/v1/meeting/{meetNo}/ai
                "callbackKey", callbackKey       // FastAPI가 Spring 콜백 호출할 때 헤더로 실어줌
        );

        // ✅ 일단 단순 호출(운영이면 @Async + retry/queue 추천)
        wc.post()
                .uri("/ai/meetings/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("[AI] requested meetNo={}, res={}", meetNo, res))
                .doOnError(e -> log.error("[AI] request failed meetNo={}", meetNo, e))
                .subscribe();
    }
}