package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 메일 상세정보 반환 Dto
 *
 * @author : 정종원
 * @filename : ResMailDetailDto
 * @since : 2025-12-30 화요일
 */
public record ResMailDetailDto(
        Long mailNo,        // ✅ 상세/상태변경 키
        String mailId,      // (선택) 내부 식별/디버그용
        String title,

        // 렌더용: JSON + HTML
        String cnttJson,
        String cnttHtml,

        String senderEmpId,
        String senderEmpName,

        String receivers,
        List<String> receiverEmpIds,
        List<String> receiverNames,

        MailRole role,
        boolean isRead,
        boolean isPrior,
        LocalDateTime deletedAt,
        LocalDateTime createdAt
) {}