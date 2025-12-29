package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * 문서양식 상세 응답 DTO
 *
 * - cnttJson: TipTap JSON (응답 시 JsonNode로 내려가서 프론트에서 JSON.parse 불필요)
 * - cnttHtml: 저장된 HTML(있다면 fallback/미리보기 용)
 * - categories: 카테고리 이름 리스트
 */
public record ResDocumentFormDetailDto(
        Long docfoNo,
        String docfoName,
        String cnttJson,
        String cnttHtml,
        String rejectReason,
        List<ResDocumentFormCategoryNameDto> categories
) {
}