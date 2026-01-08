package com.multi.mlpenterpriseapprovalsystem.common.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
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
@Slf4j
public class MeetingAiClient {

    private final WebClient fastApiWebClient; // 전역 Bean 사용

    public MeetingAiClient(@Qualifier("fastApiWebClient") WebClient fastApiWebClient) {
        this.fastApiWebClient = fastApiWebClient;
    }

    @Value("${internal.ai.meetings.callback-url}")
    private String callbackUrlTemplate;

    @Value("${internal.ai.callback-key}")
    private String callbackKey;

    public void requestAi(Long meetNo, String objectKey, String title) {
        String callbackUrl = String.format(callbackUrlTemplate, meetNo);

        Map<String, Object> body = new HashMap<>();
        body.put("meetNo", meetNo);
        body.put("objectKey", objectKey);
        body.put("callbackUrl", callbackUrl);
        body.put("callbackKey", callbackKey);
        body.put("meetingTitle", title.trim());

        fastApiWebClient.post()
                .uri("/ai/meetings/run")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("[AI] requested meetNo={}, res={}", meetNo, res))
                .block(Duration.ofSeconds(10));
    }
}