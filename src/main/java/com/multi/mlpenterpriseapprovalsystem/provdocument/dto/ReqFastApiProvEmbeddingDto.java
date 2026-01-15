package com.multi.mlpenterpriseapprovalsystem.provdocument.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 임베딩 요청 reqDto
 *
 * @author : 김승기
 * @filename : ReqFastApiProvEmbeddingDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@Builder
public class ReqFastApiProvEmbeddingDto {
    private Long provNo;
    private String comId;
    private String objectKey;     // S3 object key
    private String downloadUrl;   // (옵션) presigned GET URL
    private String originalName;
    private String contentType;
    private Long size;
    private String callbackUrl;
    private Boolean isPublic;
}