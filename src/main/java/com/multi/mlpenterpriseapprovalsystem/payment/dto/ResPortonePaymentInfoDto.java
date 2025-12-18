package com.multi.mlpenterpriseapprovalsystem.payment.dto;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.payment.enums.PaymType;
import lombok.Data;

/**
 * 포트원서버에 결제 정보 조회 API요청했을 때 응답받는 Dto
 *
 * @author : 이지헌
 * @filename : PortonePaymentResponse
 * @since : 25. 12. 17. 수요일
 */

@Data
public class ResPortonePaymentInfoDto {
    private String status; // 'paid', 'ready' 등
    private Integer amount; // 실제 결제 금액
    private String impUid; // 포트원 결제 건 ID
    private String customerUid; // 발급된 빌링키
    private String payMethod; // 'card'
    private String cardName; // 카드사명 (PaymentMethod.cardType)
    private String cardNumber; // 마스킹된 카드 번호 (PaymentMethod.mask)

    // PaymentMethod 엔티티 생성을 위한 유틸리티 메서드
    public PaymentMethod toEntity(Company company) {
        return PaymentMethod.builder()
                .company(company)
                .paymType(PaymType.C) // 현재는 카드만 구현
                .cardType(this.cardName)
                .billingKey(this.customerUid)
                .mask(this.cardNumber) // cardNumber를 마스크 번호로 사용
                .active(true)
                .build();
    }
}
