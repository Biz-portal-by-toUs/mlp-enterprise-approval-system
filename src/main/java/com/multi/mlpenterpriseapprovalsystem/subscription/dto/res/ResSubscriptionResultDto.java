package com.multi.mlpenterpriseapprovalsystem.subscription.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 요금제 결제 결과 응답 Dto
 *
 * @author : 이지헌
 * @filename : ResSubscriptionResultDto
 * @since : 26. 1. 8. 목요일
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResSubscriptionResultDto {
    private BigDecimal paidAmount;
    private BigDecimal usedCredit;   // 예치금에서 차감된 금액
    private String status; // "PAYMENT_COMPLETED", "DEPOSIT_ONLY", "RESERVATION_COMPLETED"
}
