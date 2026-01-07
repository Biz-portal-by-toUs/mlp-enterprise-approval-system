package com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res;

import com.multi.mlpenterpriseapprovalsystem.company.domain.*;

/**
 * 문서 양식 수정 내용 반환 Dto
 *
 * @author : 정종원
 * @filename : DocumentFormUpdateResDto
 * @since : 2025-12-22 월요일
 */

public record ResDocumentFormUpdateDto(
        Company company,
        Long newDocfoNo
) {}