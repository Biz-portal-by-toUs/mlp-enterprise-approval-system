package com.multi.mlpenterpriseapprovalsystem.payment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제내역 응답 Dto
 *
 * @author : 이지헌
 * @filename : ResPaymentHistoryDto
 * @since : 26. 1. 8. 목요일
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResPaymentHistoryDto {

    private Long payhNo;
    private BigDecimal amount;
    private Boolean payResult;

    private Long paymentMethodNo; // 결제수단 식별자
    private String cardType; // 카드사 일음
    private String mask; // 카드 마스킹 번호

    private LocalDateTime createdAt;
}
