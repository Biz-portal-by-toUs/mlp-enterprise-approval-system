package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import java.time.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ResMailDraftSavedDto
 * @since : 2026-01-07 수요일
 */
public record ResMailDraftSavedDto(
        String mailId,
        Long mailNo,
        LocalDateTime savedAt
) {}
