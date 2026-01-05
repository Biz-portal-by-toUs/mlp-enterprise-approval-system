package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req;

import java.util.*;

/**
 * Please explain the class!!!
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
