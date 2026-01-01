package com.multi.mlpenterpriseapprovalsystem.document.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : DocumentWebClientConfig
 * @since : 25. 12. 28. 일요일
 */
@Configuration
@RequiredArgsConstructor
public class DocumentWebClientConfig {

    private final DocumentOpenAiConfig documentOpenAiConfig;


    @Bean
    public WebClient documentOpenAiWebClient() {
        return WebClient.builder()
                .baseUrl(documentOpenAiConfig.getUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + documentOpenAiConfig.getKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
