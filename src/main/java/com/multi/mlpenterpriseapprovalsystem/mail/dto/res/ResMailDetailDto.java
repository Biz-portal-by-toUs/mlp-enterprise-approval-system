package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;

import java.time.*;

/**
 * 메일 상세정보 반환 Dto
 *
 * @author : 정종원
 * @filename : ResMailDetailDto
 * @since : 2025-12-30 화요일
 */

public record ResMailDetailDto(
        String mailId,
        String title,

        // 렌더용: JSON + HTML
        String cnttJson,
        String cnttHtml,

        String senderEmpId,
        String senderEmpName,

        String receivers,

        MailRole role,
        boolean isRead,
        boolean isPrior,
        LocalDateTime deletedAt,
        LocalDateTime createdAt
) {}