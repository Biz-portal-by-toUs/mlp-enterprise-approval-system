package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;

import java.time.LocalDateTime;

/**
 * 메일 목록 조회 반환 Dto
 *
 * @author : 정종원
 * @filename : ResMailListDto
 * @since : 2025-12-30 화요일
 */
public record ResMailListDto(
        Long mailNo,        // ✅ 상세/링크 키
        String mailId,      // (선택) 내부 식별/디버그용 - 화면에 노출만 안 하면 됨
        String title,

        // 발신자 정보
        String senderEmpId,
        String senderName,

        // 수신자 리스트
        String receivers,

        // 내 메일함 상태
        MailRole role,
        boolean isRead,
        boolean isPrior,
        LocalDateTime deletedAt,

        // 정렬/표시용
        LocalDateTime createdAt
) {}