package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import java.time.*;

/**
 * 메일 발송 반환 Dto
 *
 * @author : 정종원
 * @filename : ResMailSendDto
 * @since : 2025-12-30 화요일
 */

public record ResMailSendDto(
        String mailId,
        Long mailNo,
        LocalDateTime createdAt
) {}