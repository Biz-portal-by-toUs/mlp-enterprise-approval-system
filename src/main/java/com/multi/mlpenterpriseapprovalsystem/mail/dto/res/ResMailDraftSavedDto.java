package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import java.time.LocalDateTime;

/**
 * 임시저장된 메일 조회 Dto
 *
 * @author : 정종원
 * @filename : ResMailDraftSavedDto
 * @since : 2026-01-07 수요일
 */
public record ResMailDraftSavedDto(
        Long mailNo,        // ✅ draft 편집/상세 진입 키로도 가능
        String mailId,      // (선택)
        LocalDateTime savedAt
) {}