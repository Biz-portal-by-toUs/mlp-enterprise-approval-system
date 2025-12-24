package com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res;

import com.multi.mlpenterpriseapprovalsystem.company.domain.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormListResDto
 * @since : 2025-12-22 월요일
 */

public record ResDocumentFormListDto(
        Long docfoNo,
        Company company,
        String docfoName,
        DocumentFormStats docfoStat
) {}