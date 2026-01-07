package com.multi.mlpenterpriseapprovalsystem.provdocument.dto;

import com.multi.mlpenterpriseapprovalsystem.provdocument.domain.ProvProcStat;
import lombok.Builder;
import lombok.Getter;

/**
 * presign 발급 reqDto
 *
 * @author : 김승기
 * @filename : ResProvDocumentCreateDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@Builder
public class ResProvDocumentCreateDto {
    private Long provNo;

    private String objectKey;
    private String uploadUrl;

    private ProvProcStat procStat;
}