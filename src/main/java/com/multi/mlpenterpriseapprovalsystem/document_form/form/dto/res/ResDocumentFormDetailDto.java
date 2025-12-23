package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res;

import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.*;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormDetailResDto
 * @since : 2025-12-22 월요일
 */

public record ResDocumentFormDetailDto(
        Long docfoNo,
        String docfoName,
        DocumentFormStats docfoStat,
        String cnttHtml,
        String cnttJson,
        List<String> categories
) {}
