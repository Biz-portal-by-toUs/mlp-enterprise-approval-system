package com.multi.mlpenterpriseapprovalsystem.document.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAi 설정
 *
 * @author : 이지헌
 * @filename : DocumentOpenAiConfig
 * @since : 25. 12. 28. 일요일
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "document.openai.api")
public class DocumentOpenAiConfig {
    private String key;
    private String url;
    private String model;
    private Integer maxTokens;
    private Double temperature;
}
