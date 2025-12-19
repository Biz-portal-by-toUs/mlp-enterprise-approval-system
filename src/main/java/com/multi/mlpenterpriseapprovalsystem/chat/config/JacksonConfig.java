package com.multi.mlpenterpriseapprovalsystem.chat.config;

/**
 * JSON 처리를 위한 Jackson 라이브러리 설정
 *
 * @author : 김승기
 * @filename : JacksonConfig
 * @since : 2025. 12. 17. 수요일
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // LocalDateTime 대비
        return mapper;
    }
}