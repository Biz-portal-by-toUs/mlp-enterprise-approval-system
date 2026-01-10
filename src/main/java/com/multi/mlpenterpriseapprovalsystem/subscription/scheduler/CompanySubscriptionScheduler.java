package com.multi.mlpenterpriseapprovalsystem.subscription.scheduler;

import com.multi.mlpenterpriseapprovalsystem.payment.service.PortoneService;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.CompanySubscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.CompanySubscriptionRepository;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : CompanySubscriptionSchedule
 * @since : 26. 1. 2. 금요일
 */

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CompanySubscriptionScheduler {

    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final PortoneService portoneService;
    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void processSubscriptionBilling() {
        LocalDateTime now = LocalDateTime.now();
        List<CompanySubscription> targets = companySubscriptionRepository
                .findAllByNextBillingDateBetween(now.with(LocalTime.MIN), now.with(LocalTime.MAX));

        for (CompanySubscription sub : targets) {
            String comId = sub.getCompany().getComId();
            int currentEmpCnt = sub.getCompany().getEmpCnt();

            try {
                // 결제 혹은 전환할 타겟 요금제 결정
                Subscription targetPlan = sub.isAutoRenewal() ? sub.getSubscription() : sub.getPendingSubscription();
                if (targetPlan == null) targetPlan = subscriptionRepository.findById(1L).get(); // 기본 무료

                // 인원수 체크
                if (currentEmpCnt > targetPlan.getSubLimit()) {
                    log.warn("[인원 초과] 기존 요금제 강제 유지 및 연장: {}", comId);
                    targetPlan = sub.getSubscription(); // 기존 요금제로 강제 회귀
                    sub.clearPendingPlan();
                }

                // --- [예치금 적용 로직] ---
                BigDecimal monthlyPrice = targetPlan.getSubPrice();
                // 실제 결제액 = 이번 달 요금 - 보유 예치금
                BigDecimal finalAmount = monthlyPrice.subtract(sub.getCreditBalance()).max(BigDecimal.ZERO);

                // 예치금 차감 (이번 달 요금만큼 소진)
                sub.useCredit(monthlyPrice);
                // ------------------------

                if (finalAmount.compareTo(BigDecimal.ZERO) == 0) {
                    // 예치금으로 전액 충당 시 결제 API 호출 없이 연장
                    log.info("[정기 결제] 예치금 소진 처리 완료 (결제액 0원): {}", comId);
                    sub.updatePlan(targetPlan);
                    sub.renew(LocalDateTime.now().plusMonths(1));
                } else {
                    // 예치금 소진 후 남은 금액만 실제 결제 요청
                    String merchantUid = "BILL_" + comId + "_" + System.currentTimeMillis();
                    Map<String, Object> response = portoneService.requestrecurrentPayment(
                            sub.getPaymentMethod().getBillingKey(), finalAmount, merchantUid, targetPlan.getSubName()
                    );

                    if ((Integer) response.get("code") == 0) {
                        sub.updatePlan(targetPlan);
                        sub.renew(LocalDateTime.now().plusMonths(1));
                        log.info("[정기 결제 성공] 회사: {}, 결제금액: {}", comId, finalAmount);
                    } else {
                        throw new Exception("포트원 결제 실패");
                    }
                }
                sub.clearPendingPlan(); // 처리 완료 후 예약 정보 삭제

            } catch (Exception e) {
                log.error("[스케줄러 실패] 회사: {}, 사유: {}", comId, e.getMessage());
                // 실패 시 autoRenewal을 false로 바꾸거나 알림 발송 로직 추가 권장
            }
        }
    }
}
