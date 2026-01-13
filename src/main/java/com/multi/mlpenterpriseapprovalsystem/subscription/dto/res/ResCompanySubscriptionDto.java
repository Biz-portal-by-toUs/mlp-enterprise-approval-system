package com.multi.mlpenterpriseapprovalsystem.subscription.dto.res;

import com.multi.mlpenterpriseapprovalsystem.subscription.enums.SubStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회사요금제정보 응답 Dto
 *
 * @author : 이지헌
 * @filename : ResCompanySubscriptionDto
 * @since : 26. 1. 4. 일요일
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResCompanySubscriptionDto {
    private Integer subNo;             // 현재 요금제 번호 (강조를 위한 핵심 필드)
    private String subName;         // 요금제 이름
    private SubStatus status;       // 구독 상태 (ACTIVE, FREE, CANCELED 등)
    private LocalDateTime nextBillingDate; // 다음 결제일
    private boolean autoRenewal;    // 자동 갱신 여부
    private Integer pendingSubNo;  // 예약된 요금제 정보
    private String pendingSubName; // 예약된 요금제 정보

    private Integer empCnt;        // 현재 회사의 사원 수
    private Integer pendingLimit;  // 예약된 요금제의 인원 한도
    private BigDecimal creditBalance; // 예치금

}
