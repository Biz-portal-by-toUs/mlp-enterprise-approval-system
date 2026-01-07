package com.multi.mlpenterpriseapprovalsystem.provdocument.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 규정 업데이트 reqDto
 *
 * @author : 김승기
 * @filename : ReqProvDocumentUpdateDto
 * @since : 2025. 12. 30. 화요일
 */
@Getter
@NoArgsConstructor
public class ReqProvDocumentUpdateDto {

    @NotNull
    private Long provNo;

    @Size(max = 100)
    private String docTitle;

    @Size(max = 500)
    private String description;

    private Boolean isPublic;
}