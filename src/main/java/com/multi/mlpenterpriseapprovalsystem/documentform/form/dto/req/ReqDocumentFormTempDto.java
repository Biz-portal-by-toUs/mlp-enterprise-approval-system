package com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.req;

import java.util.*;

/**
 * 문서 양식 임시 저장 요청 Dto
 *
 * @author : 정종원
 * @filename : ReqDocumentFormTempDto
 * @since : 2026-01-04 일요일
 */
public record ReqDocumentFormTempDto(
        String docfoName,
        String cnttJson,
        String cnttHtml,
        List<String> categories
) {}
