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
        String disposition = "inline";
        return presignGet(a, disposition);
    }

    public AttachmentDto.PresignedUrlResponse issueDownloadUrl(String comId, Long attachmentId) {
        Attachment a = findActive(comId, attachmentId);
        String disposition = "attachment; filename=\"" + safeFileName(a.getOriginalName()) + "\"";
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
                a.getSize(),
                a.getObjectKey()
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

    private String safeFileName(String name) {
        if (name == null || name.isBlank()) return "file";
        return name.replace("\"", "");
    }
}
