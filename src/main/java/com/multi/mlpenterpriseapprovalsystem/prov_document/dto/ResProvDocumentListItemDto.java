package com.multi.mlpenterpriseapprovalsystem.prov_document.dto;

import com.multi.mlpenterpriseapprovalsystem.prov_document.domain.ProvProcStat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사내 규정 전체 조회 item resDto
 *
 * @author : 김승기
 * @filename : ResProvDocumentListItemDto
 * @since : 2025. 12. 30. 화요일
 */

@Getter
@Builder
public class ResProvDocumentListItemDto {

    private Long provNo;

    private String docTitle;
    private String description;
    private Boolean isPublic;

    private String fileName;
    private Long fileSize;


    private ProvProcStat procStat;
    private Integer chunkCnt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}