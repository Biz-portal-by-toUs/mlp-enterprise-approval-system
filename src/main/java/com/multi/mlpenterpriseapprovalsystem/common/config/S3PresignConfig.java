package com.multi.mlpenterpriseapprovalsystem.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
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
    public AwsCredentialsProvider awsCredentialsProvider(
            @Value("${aws.access-key}") String accessKey,
            @Value("${aws.secret-key}") String secretKey
    ) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
    }

    @Bean
    public S3Presigner s3Presigner(
            @Value("${app.s3.region}") String region,
            AwsCredentialsProvider awsCredentialsProvider
    ) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    // ✅ 추가: 서버에서 S3 삭제/조회 등에 쓰는 클라이언트
    @Bean
    public S3Client s3Client(
            @Value("${app.s3.region}") String region,
            AwsCredentialsProvider awsCredentialsProvider
    ) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }
}