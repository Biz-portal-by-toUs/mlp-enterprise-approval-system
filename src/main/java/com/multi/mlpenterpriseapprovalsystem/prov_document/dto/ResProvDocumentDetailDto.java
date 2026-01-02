package com.multi.mlpenterpriseapprovalsystem.prov_document.dto;

import com.multi.mlpenterpriseapprovalsystem.prov_document.domain.ProvProcStat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사내 규정 상세 조회 resDto
 *
 * @author : 김승기
 * @filename : ResProvDocumentDetailDto
 * @since : 2025. 12. 30. 화요일
 */

@Getter
@Builder
public class ResProvDocumentDetailDto {

    private Long provNo;

    private String docTitle;
    private String description;
    private Boolean isPublic;

    private String fileName;
    private String objectKey;
    private Long fileSize;

    private Integer chunkCnt;
    private ProvProcStat procStat;
    private String errorMsg;

    private String downloadUrl; // presigned GET

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}