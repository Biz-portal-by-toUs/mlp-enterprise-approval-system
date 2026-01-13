package com.multi.mlpenterpriseapprovalsystem.subscription.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.subscription.enums.SubStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 회사요금제정보 엔티티
 *
 * @author : 이지헌
 * @filename : CompanySubscription
 * @since : 26. 1. 2. 금요일
 */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
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
    @JoinColumn(name = "paym_no", nullable = true)
    private PaymentMethod paymentMethod; // 결제에 사용할 카드(빌링키)

    @Column(name = "next_billing_date", nullable = true)
    private LocalDateTime nextBillingDate; // 다음 결제(갱신) 예정일

    @Column(name = "auto_renewal", nullable = false)
    private boolean autoRenewal = true; // 자동 갱신 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_stat", nullable = false)
    private SubStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_sub_no")
    private Subscription pendingSubscription; // 만료 후 변경될 예약 요금제

    @Column(name = "credit_balance", nullable = false)
    @Builder.Default
    private BigDecimal creditBalance = BigDecimal.ZERO; // 예치금

    /**
     * 변경 예약 또는 해지 예약을 취소하고 기존 상태로 복구
     */
    public void resumeSubscription() {
        // 상태가 FREE(완전 종료)인 경우에는 복구가 불가능하므로 서비스 레이어에서 체크 필요
        this.status = SubStatus.ACTIVE;
        this.autoRenewal = true;
        this.pendingSubscription = null; // 예약된 요금제 제거
    }

    // 예치금 추가 메서드
    public void addCredit(BigDecimal amount) {
        this.creditBalance = this.creditBalance.add(amount);
    }

    // 예치금 사용 메서드 (남은 예치금 반환)
    public void useCredit(BigDecimal amount) {
        this.creditBalance = this.creditBalance.subtract(amount).max(BigDecimal.ZERO);
    }

    // 요금제 예약 메서드 (다운그레이드용)
    public void reservePlanChange(Subscription nextPlan) {
        this.pendingSubscription = nextPlan;
        this.autoRenewal = false; // 예약 변경이므로 다음 자동 결제는 막음
        this.status = SubStatus.CANCELED; // 해지 예약과 유사한 상태로 취급
    }

    // 업그레이드 시 예약 정보 초기화
    public void clearPendingPlan() {
        this.pendingSubscription = null;
    }

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

    public static CompanySubscription toEntity(Company company,
                                        Subscription plan,
                                        PaymentMethod paymentMethod,
                                        LocalDateTime nextBillingDate,
                                        Boolean autoRenewal,
                                        SubStatus status) {
        return CompanySubscription.builder()
                .company(company)
                .subscription(plan)
                .paymentMethod(paymentMethod)
                .nextBillingDate(nextBillingDate)
                .autoRenewal(autoRenewal)
                .status(status)
                .build();
    }
}
