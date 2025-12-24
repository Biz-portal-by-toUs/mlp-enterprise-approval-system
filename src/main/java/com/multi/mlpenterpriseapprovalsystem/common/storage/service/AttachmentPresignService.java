package com.multi.mlpenterpriseapprovalsystem.common.storage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * 첨부파일 s3 저장 경로 만드는 서비스
 *
 * @author : 권지영
 * @filename : AttachmentPresignService
 * @since : 2025. 12. 23. 화요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentPresignService {

    private final S3Presigner presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.env}")
    private String env;

    public PresignResponse presignPut(PresignRequest req, String comId) {
        // 1) 여기서 권한 체크 + (PK당 최대 5개) 제한 체크를 보통 함

        // 2) objectKey 생성
        String safeName = req.originalName().replaceAll("\\s+", "_");
        String objectKey = String.format(env + "/%s/%s/%s/%s/%s_%s",
                comId,
                req.domain().toLowerCase(),                 // notice, mail ...
                req.domain().toLowerCase() + "-" + req.entityId(), // notice-88
                req.fileType().toLowerCase(),               // image/doc/audio
                UUID.randomUUID(),
                safeName
        );

        // 3) presigned PUT URL 생성 (만료 짧게: 3~10분 추천)
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(req.contentType())
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putReq)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignReq);

        log.info("[presign] objectKey={}", objectKey);

        return new PresignResponse(objectKey, presigned.url().toString());
    }

    public record PresignRequest(String domain, Long entityId, String fileType,
                                 String originalName, String contentType, Long size) {}
    public record PresignResponse(String objectKey, String uploadUrl) {}
}
