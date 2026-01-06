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

    /**
     * 매일 새벽 2시에 실행되는 정기 결제 및 만료 처리 스케줄러
     */
    // 0 0 2 * * *
//    @Scheduled(cron = "0 0 2 * * *") // 매일 02:00:00 실행
//    public void processSubscriptionBilling() {
//        LocalDateTime now = LocalDateTime.now();
//        // 오늘의 시작(00:00:00)부터 끝(23:59:59)까지의 범위를 설정
//        LocalDateTime startOfDay = now.with(LocalTime.MIN);
//        LocalDateTime endOfDay = now.with(LocalTime.MAX);
//
//        log.info("[스케줄러 시작] 결제 대상 조회 범위: {} ~ {}", startOfDay, endOfDay);
//
//        // 1. 오늘 결제 예정인 구독 정보 조회
//        List<CompanySubscription> targets = companySubscriptionRepository
//                .findAllByNextBillingDateBetween(startOfDay, endOfDay);
//
//        if (targets.isEmpty()) {
//            log.info("[스케줄러] 오늘 처리할 결제 대상이 없습니다.");
//            return;
//        }
//
//        for (CompanySubscription sub : targets) {
//            String comId = sub.getCompany().getComId();
//
//            try {
//                if (sub.isAutoRenewal()) {
//                    // CASE A: 자동 갱신인 경우 -> PortoneService를 통해 결제 시도
//                    log.info("[자동 결제 시도] 회사: {}, 요금제: {}", comId, sub.getSubscription().getSubName());
//
//                    // 기존에 만든 subscribeProPlan 호출 (내부에서 결제 + 연장 + 이력저장 수행)
//                    portoneService.subscribeProPlan(comId, sub.getSubscription().getSubNo().longValue());
//
//                    log.info("[자동 결제 성공] 회사: {}", comId);
//                } else {
//                    // CASE B: 자동 갱신이 꺼진 경우 (해지 예약 상태) -> 무료 요금제로 강등
//                    log.info("[구독 만료 처리] 회사: {}, 자동갱신 꺼짐", comId);
//
//                    Subscription freePlan = subscriptionRepository.findById(1L) // 1번: basic(free) 요금제
//                            .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));
//
//                    sub.downgradeToFree(freePlan);
//
//                    log.info("[무료 전환 완료] 회사: {}", comId);
//                }
//            } catch (Exception e) {
//                // 결제 실패 시 에러 로그를 남기고 다음 대상으로 넘어감
//                log.error("[결제 실패] 회사: {}, 사유: {}", comId, e.getMessage());
//                // TODO: 여기에 결제 실패 알림(이메일 등) 로직을 추가
//            }
//        }
//
//        log.info("[스케줄러 종료] 총 {}건 처리 완료", targets.size());
//    }
//    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시 실행
//    public void processSubscriptionBilling() {
//        LocalDateTime now = LocalDateTime.now();
//        LocalDateTime startOfDay = now.with(LocalTime.MIN);
//        LocalDateTime endOfDay = now.with(LocalTime.MAX);
//
//        log.info("[스케줄러 시작] 결제 및 만료 처리 범위: {} ~ {}", startOfDay, endOfDay);
//
//        // 오늘이 차기 결제일(만료일)인 대상 조회
//        List<CompanySubscription> targets = companySubscriptionRepository
//                .findAllByNextBillingDateBetween(startOfDay, endOfDay);
//
//        for (CompanySubscription sub : targets) {
//            String comId = sub.getCompany().getComId();
//            try {
//                if (sub.isAutoRenewal()) {
//                    // CASE A: 정상 이용 중인 경우 -> 현재 요금제로 정기 결제 진행
//                    log.info("[정기 결제] 회사: {}, 요금제: {}", comId, sub.getSubscription().getSubName());
//                    portoneService.subscribeProPlan(comId, sub.getSubscription().getSubNo().longValue());
//                }
//                else {
//                    // CASE B: 해지 예약(CANCELED) 상태인 경우 (만료일 도래)
//                    Subscription pendingPlan = sub.getPendingSubscription(); // 예약된 요금제 확인
//
//                    if (pendingPlan != null) {
//                        // 1. 예약된 요금제가 있는 경우 (유료 -> 낮은 유료로 다운그레이드)
//                        log.info("[요금제 예약 전환] 회사: {}, {} -> {}", comId, sub.getSubscription().getSubName(), pendingPlan.getSubName());
//
//                        sub.updatePlan(pendingPlan); // 요금제 교체
//                        sub.clearPendingPlan();      // 예약 정보 삭제
//
//                        // 새 요금제로 첫 정기 결제 시도 (성공 시 다시 ACTIVE 상태가 됨)
//                        portoneService.subscribeProPlan(comId, pendingPlan.getSubNo().longValue());
//                        log.info("[다운그레이드 완료 및 결제 성공] 회사: {}", comId);
//                    }
//                    else {
//                        // 2. 예약된 요금제가 없는 경우 (유료 -> 무료로 완전 해지)
//                        log.info("[구독 종료] 회사: {}, 무료 요금제 강등", comId);
//
//                        Subscription freePlan = subscriptionRepository.findById(1L)
//                                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));
//
//                        sub.downgradeToFree(freePlan);
//                        log.info("[무료 전환 완료] 회사: {}", comId);
//                    }
//                }
//            } catch (Exception e) {
//                log.error("[스케줄러 처리 실패] 회사: {}, 사유: {}", comId, e.getMessage());
//                // TODO: 실패 알림(이메일/슬랙) 로직 추가
//            }
//        }
//        log.info("[스케줄러 종료] 처리 완료");
//    }

    @Scheduled(cron = "0 0 2 * * *")
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
