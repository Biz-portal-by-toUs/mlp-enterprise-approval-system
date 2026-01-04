package com.multi.mlpenterpriseapprovalsystem.subscription.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.subscription.enums.SubStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회사의 구독정보 엔티티
 *
 * @author : 이지헌
 * @filename : CompanySubscription
 * @since : 26. 1. 2. 금요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "company_subscription")
public class CompanySubscription extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "com_sub_no")
    private Long comSubNo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_no")
    private Subscription subscription; // 요금제 정보

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paym_no")
    private PaymentMethod paymentMethod; // 결제에 사용할 카드(빌링키)

    @Column(name = "next_billing_date", nullable = true)
    private LocalDateTime nextBillingDate; // 다음 결제(갱신) 예정일

    @Column(name = "auto_renewal", nullable = false)
    private boolean autoRenewal = true; // 자동 갱신 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_stat", nullable = false)
    private SubStatus status;

    // 결제 수단 변경
    public void changePaymentMethod(PaymentMethod newMethod) {
        this.paymentMethod = newMethod;
    }

    // 구독 해지 예약 로직 (제미나이 방식의 핵심)
    public void cancelSubscription() {
        this.autoRenewal = false;
        this.status = SubStatus.CANCELED;
        // 상태는 CANCELED지만 nextBillingDate까지는 ACTIVE와 동일하게 취급
    }

    // 결제 성공 후 날짜 갱신
    public void renew(LocalDateTime newBillingDate) {
        this.nextBillingDate = newBillingDate;
        this.status = SubStatus.ACTIVE;
        this.autoRenewal = true;
    }

    // 유료 권한이 있는지 확인 (Null Safety 추가)
    public boolean hasProAccess() {
        if (this.status == SubStatus.ACTIVE) return true;

        // CANCELED 상태이면서 날짜가 아직 지나지 않았는지 확인
        return this.status == SubStatus.CANCELED &&
                this.nextBillingDate != null &&
                this.nextBillingDate.isAfter(LocalDateTime.now());
    }

    // FREE 요금제로 강등(Downgrade) 처리하는 메서드
    public void downgradeToFree(Subscription freePlan) {
        this.subscription = freePlan;
        this.status = SubStatus.FREE;
        this.nextBillingDate = null;
        this.autoRenewal = false;
        // 결제 수단(paymentMethod)은 유지할 수도, 지울 수도 있음 (다음 결제를 위해 유지 추천)
    }

    public void updatePlan(Subscription plan) {
        this.subscription = plan;
    }
}
