package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import java.time.LocalDateTime;

/**
 * 메일 발송 반환 Dto
 *
 * @author : 정종원
 * @filename : ResMailSendDto
 * @since : 2025-12-30 화요일
 */
public record ResMailSendDto(
        Long mailNo,        // ✅ 클라이언트는 mailNo로 상세 이동
        String mailId,      // (선택) 내부 식별/디버그용
        LocalDateTime createdAt
) {}