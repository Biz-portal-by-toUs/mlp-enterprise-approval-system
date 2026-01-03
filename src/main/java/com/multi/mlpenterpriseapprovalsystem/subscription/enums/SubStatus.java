package com.multi.mlpenterpriseapprovalsystem.subscription.enums;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : SubStatus
 * @since : 26. 1. 2. 금요일
 */
public enum SubStatus {
    FREE,       // 무료 요금제 이용 중
    ACTIVE,     // 유료 요금제 이용 중 (자동 갱신 활성)
    CANCELED,   // 유료 이용 중이나 해지 예약됨 (기간 종료 후 FREE로 전환 예정)
    EXPIRED     // 결제 실패 등으로 인해 유료 권한이 즉시 정지된 상태
}
