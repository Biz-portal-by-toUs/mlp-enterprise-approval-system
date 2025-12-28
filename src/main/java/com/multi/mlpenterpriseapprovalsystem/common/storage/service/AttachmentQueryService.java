package com.multi.mlpenterpriseapprovalsystem.common.storage.service;

import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.AttachmentDto;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * 첨부파일 목록 조회, 미리보기/다운로드 로직 서비스
 *
 * @author : 권지영
 * @filename : AttachmentQueryService
 * @since : 2025. 12. 24. 수요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentQueryService {

    private final AttachmentRepository attachmentRepository;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket}")
    private String bucket;

    public List<AttachmentDto.AttachmentListItem> list(String comId, AttachmentDomain domain, Long entityId) {
        return attachmentRepository
                .findByComIdAndDomainAndEntityIdAndStatusOrderByDisplayOrderAsc(
                        comId, domain, entityId, AttachmentStatus.ACTIVE
                )
                .stream()
                .map(this::toListItem)
                .toList();
    }

    public AttachmentDto.PresignedUrlResponse issuePreviewUrl(String comId, Long attachmentId) {
        Attachment a = findActive(comId, attachmentId);
        String disposition = buildContentDisposition("inline", a.getOriginalName());
        return presignGet(a, disposition);
    }

    public AttachmentDto.PresignedUrlResponse issueDownloadUrl(String comId, Long attachmentId) {
        Attachment a = findActive(comId, attachmentId);
        String disposition = buildContentDisposition("attachment", a.getOriginalName());
        return presignGet(a, disposition);
    }

    // ---------- private helpers ----------

    private Attachment findActive(String comId, Long attachmentId) {
        return attachmentRepository
                .findByAttachmentIdAndComIdAndStatus(attachmentId, comId, AttachmentStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));
    }

    private AttachmentDto.AttachmentListItem toListItem(Attachment a) {
        return new AttachmentDto.AttachmentListItem(
                a.getAttachmentId(),
                a.getDisplayOrder(),
                a.getFileType().name(),
                a.getOriginalName(),
                a.getContentType(),
                a.getSize()
        );
    }

    private AttachmentDto.PresignedUrlResponse presignGet(Attachment a, String contentDisposition) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(a.getObjectKey())
                .responseContentType(a.getContentType())
                .responseContentDisposition(contentDisposition)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(p -> p
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(getReq)
        );

        return new AttachmentDto.PresignedUrlResponse(
                a.getAttachmentId(),
                a.getOriginalName(),
                a.getContentType(),
                a.getSize(),
                presigned.url().toString()
        );
    }

    /**
     * RFC 5987 방식으로 Content-Disposition 헤더 생성
     * 한글 등 non-ASCII 문자를 안전하게 처리
     */
    private String buildContentDisposition(String type, String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return type + "; filename=\"file\"";
        }

        // ASCII만 포함된 경우
        if (isAsciiOnly(originalName)) {
            String safeName = originalName.replace("\"", "");
            return type + "; filename=\"" + safeName + "\"";
        }

        // 한글 등 non-ASCII 포함: RFC 5987 인코딩 사용
        String encodedName = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return type + "; filename=\"file\"; filename*=UTF-8''" + encodedName;
    }

    private boolean isAsciiOnly(String str) {
        for (char c : str.toCharArray()) {
            if (c > 127) return false;
        }
        return true;
    }
}
