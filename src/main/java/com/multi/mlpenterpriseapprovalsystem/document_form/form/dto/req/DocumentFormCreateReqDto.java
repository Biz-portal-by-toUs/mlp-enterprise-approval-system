package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormSaveReqDto
 * @since : 2025-12-22 월요일
 */

public record DocumentFormCreateReqDto(
        String comId,
        String writerId,
        String docfoName,
        String cnttHtml,
        String cnttJson,
        List<String> categories
) {}
