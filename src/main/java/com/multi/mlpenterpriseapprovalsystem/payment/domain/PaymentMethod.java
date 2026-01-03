package com.multi.mlpenterpriseapprovalsystem.payment.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.payment.enums.PaymType;
import jakarta.persistence.*;
import lombok.*;

/**
 * 결제 수단 엔티티
 *
 * @author : 이지헌
 * @filename : PaymentMethod
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment_method")
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentMethod extends BaseEntity {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long paymNo;
  
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "com_id", referencedColumnName = "com_id") 
    private Company company;
    

    @Enumerated(EnumType.STRING)
    private PaymType paymType;

    private String cardType;
    private String billingKey;
    private String mask;

    @Builder.Default
    private Boolean active = true;

    // 소프트 삭제
    public void deactivate() {
        this.active = false;
    }
}
