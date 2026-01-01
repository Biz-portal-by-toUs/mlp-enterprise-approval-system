package com.multi.mlpenterpriseapprovalsystem.prov_document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 규정 업로드 완료 reqDto
 *
 * @author : 김승기
 * @filename : ReqProvDocumentCompleteDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@Setter
public class ReqProvDocumentCompleteDto {

    @NotNull
    private Long provNo;

    @NotBlank
    private String objectKey;

    @NotBlank
    private String originalName;

    @NotBlank
    private String contentType;

    @NotNull
    private Long size;
}