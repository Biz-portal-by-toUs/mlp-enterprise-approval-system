package com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res;

import com.multi.mlpenterpriseapprovalsystem.company.domain.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormUpdateResDto
 * @since : 2025-12-22 월요일
 */

public record ResDocumentFormUpdateDto(
        Company company,
        Long newDocfoNo
) {}