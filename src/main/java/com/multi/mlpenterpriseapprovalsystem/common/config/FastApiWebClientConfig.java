package com.multi.mlpenterpriseapprovalsystem.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * fastAPI와 연결하는 클라이언트
 *
 * @author : 김승기
 * @filename : FastApiWebClientConfig
 * @since : 2025. 12. 29. 월요일
 */
@Configuration
public class FastApiWebClientConfig {

    @Bean
    public WebClient fastApiWebClient(@Value("${ai.fastapi.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter(logRequest())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return (request, next) -> {
            System.out.println("[FASTAPI] " + request.method() + " " + request.url());
            return next.exchange(request);
        };
    }
}