package com.multi.mlpenterpriseapprovalsystem.mail.dto.req;

/**
 * 메일 임시 저장 요청 Dto
 *
 * @author : 정종원
 * @filename : ReqMailDraftSaveDto
 * @since : 2026-01-07 수요일
 */

public record ReqMailDraftSaveDto(
        String mailId,     // null이면 신규 임시저장 생성, 있으면 해당 초안 업데이트
        String title,
        String cnttJson
) {}
