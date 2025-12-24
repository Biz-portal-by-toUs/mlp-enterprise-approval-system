package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req;

import com.multi.mlpenterpriseapprovalsystem.company.domain.*;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;

import java.util.*;

/**
 * Please explain the class!!!
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
