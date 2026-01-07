package com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.req;

import java.util.*;

/**
 * 문서 양식 생성/수정 요청 Dto
 *
 * @author : 정종원
 * @filename : DocumentFormSaveReqDto
 * @since : 2025-12-22 월요일
 */

public record ReqDocumentFormCreateDto(
        String docfoName,
        String cnttHtml,
        String cnttJson,
        List<String> categories
) {}
