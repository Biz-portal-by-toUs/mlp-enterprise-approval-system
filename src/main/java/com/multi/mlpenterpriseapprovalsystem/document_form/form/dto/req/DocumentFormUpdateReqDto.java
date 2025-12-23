package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormUpdateReqDto
 * @since : 2025-12-22 월요일
 */

public record DocumentFormUpdateReqDto(
        String writerId,
        String docfoName,
        String cnttJson,
        String cnttHtml,
        List<String> categories
) {}
