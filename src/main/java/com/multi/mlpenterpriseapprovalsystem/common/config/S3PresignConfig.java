package com.multi.mlpenterpriseapprovalsystem.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * s3 key 제작 config
 *
 * @author : 권지영
 * @filename : S3presignConfig
 * @since : 2025. 12. 23. 화요일
 */
@Configuration
public class S3PresignConfig {
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.US_WEST_1) // 버킷 리전과 반드시 동일!
                .build();
    }
}
