package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.*;

import java.util.List;

/**
 * 문서양식 상세 응답 DTO
 *
 * @author : 정종원
 * @filename : ResDocumentFormCategoryNameDto
 * @since : 2025-12-24 수요일
 */
public record ResDocumentFormDetailDto(
        Long docfoNo,
        String docfoName,
        String cnttJson,
        String cnttHtml,
        String rejectReason,
        DocumentFormStats docfoStat,
        List<ResDocumentFormCategoryNameDto> categories
) {
}