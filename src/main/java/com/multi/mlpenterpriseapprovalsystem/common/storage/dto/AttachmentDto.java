package com.multi.mlpenterpriseapprovalsystem.common.storage.dto;

/**
 * 첨부파일 관련 dto
 *
 * @author : 권지영
 * @filename : AttachmentDto
 * @since : 2025. 12. 24. 수요일
 */
public class AttachmentDto {

    public record PresignRequest(String domain, Long entityId, String fileType,
                                 String originalName, String contentType, Long size) {}
    public record PresignResponse(String objectKey, String uploadUrl) {}

    public record AttachmentListItem(
            Long attachmentId,
            Integer displayOrder,
            String fileType,
            String originalName,
            String contentType,
            Long size
    ) {}

    public record PresignedUrlResponse(
            Long attachmentId,
            String originalName,
            String contentType,
            Long size,
            String url
    ) {}

    public record CompleteRequest(
            String domain,
            Long entityId,
            String fileType,
            String objectKey,
            String originalName,
            String contentType,
            Long size,
            String etag // 선택: 프론트가 보내면 받고, 아니면 null
    ) {}

    public record CompleteResponse(
            Long attachmentId,
            Integer displayOrder
    ) {}
}
