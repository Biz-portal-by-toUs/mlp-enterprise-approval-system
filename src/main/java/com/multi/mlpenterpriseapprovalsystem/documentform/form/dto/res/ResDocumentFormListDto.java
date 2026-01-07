package com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res;

import com.fasterxml.jackson.annotation.*;
import com.multi.mlpenterpriseapprovalsystem.company.domain.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.enums.*;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;

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
        @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
        Employee writer,
        String docfoName,
        DocumentFormStats docfoStat,
        String rejectReason
) {}