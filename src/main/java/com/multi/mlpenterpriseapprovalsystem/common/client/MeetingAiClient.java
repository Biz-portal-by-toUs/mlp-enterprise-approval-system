package com.multi.mlpenterpriseapprovalsystem.common.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
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

    public void requestAi(Long meetNo, String objectKey, String title) {

        WebClient wc = webClientBuilder.baseUrl(fastApiBaseUrl).build();

        Map<String, Object> body = new HashMap<>();
        body.put("meetNo", meetNo);
        body.put("objectKey", objectKey);
        body.put("callbackUrl", callbackUrl);
        body.put("callbackKey", callbackKey);


        body.put("meetingTitle", title.trim());


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