package com.multi.mlpenterpriseapprovalsystem.payment.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 내역 엔티티
 *
 * @author : 이지헌
 * @filename : PaymentHistory
 * @since : 2025. 12. 16. 화요일
 */
@Entity @Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "payment_history")
public class PaymentHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long payhNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    private BigDecimal amount;
    private Boolean payResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paym_no")
    private PaymentMethod paymentMethod;

    @CreatedDate
    private LocalDateTime createdAt;

    public PaymentHistory(Company company, BigDecimal subPrice, boolean payResult, PaymentMethod paymentMethod) {
        this.company = company;
        this.amount = subPrice;
        this.payResult = payResult;
        this.paymentMethod = paymentMethod;
    }
}