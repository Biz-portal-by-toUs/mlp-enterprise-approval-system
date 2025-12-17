package com.multi.mlpenterpriseapprovalsystem.chat.redis;

/**
 * Please explain the class!!!
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