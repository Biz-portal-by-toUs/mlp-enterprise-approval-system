package com.multi.mlpenterpriseapprovalsystem.subscription.dto.response;

import com.multi.mlpenterpriseapprovalsystem.subscription.enums.SubStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회사의 요금제 정보 응답 dto
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
}
