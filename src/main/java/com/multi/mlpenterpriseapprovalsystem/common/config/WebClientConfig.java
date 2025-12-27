package com.multi.mlpenterpriseapprovalsystem.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 외부 API 호출 WebClient
 *
 * @author : 김승기
 * @filename : WebClientConfig
 * @since : 2025. 12. 26. 금요일
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}