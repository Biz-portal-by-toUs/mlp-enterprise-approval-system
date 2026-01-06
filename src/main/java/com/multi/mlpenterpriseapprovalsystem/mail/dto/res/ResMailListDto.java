package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;

import java.time.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ResMailListDto
 * @since : 2025-12-30 화요일
 */

public record ResMailListDto (
    String mailId,
    String title,

    // 발신자 정보
    String senderEmpId,
    String senderName,

    //수신자 리스트
    String receivers,

    // 내 메일함 상태
    MailRole role,
    boolean isRead,
    boolean isPrior,
    LocalDateTime deletedAt,

    // 정렬/표시용
    LocalDateTime createdAt
){}