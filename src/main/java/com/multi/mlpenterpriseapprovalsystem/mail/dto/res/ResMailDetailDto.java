package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;

import java.time.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ResMailDetailDto
 * @since : 2025-12-30 화요일
 */
public record ResMailDetailDto(
        String mailId,
        String title,
        String cntt,              // JSON 문자열(또는 HTML이면 cnttHtml)

        // 발신자
        String senderEmpId,
        String senderName,

        // 내 상태
        MailRole myRole,
        boolean isRead,
        boolean isPrior,
        LocalDateTime deletedAt,

        LocalDateTime createdAt
) {}