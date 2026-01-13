package com.multi.mlpenterpriseapprovalsystem.document.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 문서 상태 관리 enum
 *
 * @author : 이지헌
 * @filename : DocStat
 * @since : 25. 12. 18. 목요일
 */
@Getter
@AllArgsConstructor
public enum DocStat {

    US("UNSUBMITTED"), // 상신전
    AW("AWAITING"), // 결재중
    FI("FINALIZED"), // 최종승인
    RJ("REJECTED"); // 반려

    private final String status;
}
