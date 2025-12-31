package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import java.time.*;

/**
 * Please explain the class!!!
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