package com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.req;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.enums.*;

/**
 * 문서 양식 상태 변환 요청 Dto
 *
 * @filename    : DocumentFormStatusReqDto
 * @author      : 정종원
 * @since       : 2025-12-22 월요일
 */

public record ReqDocumentFormStatusDto(
        DocumentFormStats docfoStat,
        String rejectReason
) {}
