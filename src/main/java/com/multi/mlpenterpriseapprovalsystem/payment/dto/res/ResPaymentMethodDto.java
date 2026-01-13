package com.multi.mlpenterpriseapprovalsystem.payment.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 결제수단 응답 Dto
 *
 * @author : 이지헌
 * @filename : ResPaymentMethodDto
 * @since : 26. 1. 3. 토요일
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResPaymentMethodDto {
    private Long paymNo;         // 결제 수단 고유 번호
    private String cardType;     // 카드 종류 (예: 하나카드)
    private String mask;         // 마스킹된 카드 번호

    @JsonProperty("isRepresentative")
    private boolean isRepresentative; // 대표 결제 수단 여부
}
