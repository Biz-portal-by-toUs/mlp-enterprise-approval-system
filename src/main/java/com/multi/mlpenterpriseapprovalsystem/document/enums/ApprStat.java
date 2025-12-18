package com.multi.mlpenterpriseapprovalsystem.document.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결재 라인 상태 관리 enum
 *
 * @author : 이지헌
 * @filename : ApprStat
 * @since : 25. 12. 17. 수요일
 */
@Getter
@RequiredArgsConstructor
public enum ApprStat {
    I,   // 결재중 (In progress)
    W,   // 결재대기중 (Waiting)
    A,   // 승인 (Approved)
    R;   // 반려 (Rejected)
}
