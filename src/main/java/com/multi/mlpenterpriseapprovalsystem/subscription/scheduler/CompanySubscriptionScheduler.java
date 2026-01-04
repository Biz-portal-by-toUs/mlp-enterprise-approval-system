package com.multi.mlpenterpriseapprovalsystem.subscription.scheduler;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
    @Scheduled(cron = "0 0 2 * * *") // 매일 02:00:00 실행
    public void processSubscriptionBilling() {
        LocalDateTime now = LocalDateTime.now();
        // 오늘의 시작(00:00:00)부터 끝(23:59:59)까지의 범위를 설정
        LocalDateTime startOfDay = now.with(LocalTime.MIN);
        LocalDateTime endOfDay = now.with(LocalTime.MAX);

        log.info("[스케줄러 시작] 결제 대상 조회 범위: {} ~ {}", startOfDay, endOfDay);

        // 1. 오늘 결제 예정인 구독 정보 조회
        List<CompanySubscription> targets = companySubscriptionRepository
                .findAllByNextBillingDateBetween(startOfDay, endOfDay);

        if (targets.isEmpty()) {
            log.info("[스케줄러] 오늘 처리할 결제 대상이 없습니다.");
            return;
        }

        for (CompanySubscription sub : targets) {
            String comId = sub.getCompany().getComId();

            try {
                if (sub.isAutoRenewal()) {
                    // CASE A: 자동 갱신인 경우 -> PortoneService를 통해 결제 시도
                    log.info("[자동 결제 시도] 회사: {}, 요금제: {}", comId, sub.getSubscription().getSubName());

                    // 기존에 만든 subscribeProPlan 호출 (내부에서 결제 + 연장 + 이력저장 수행)
                    portoneService.subscribeProPlan(comId, sub.getSubscription().getSubNo().longValue());

                    log.info("[자동 결제 성공] 회사: {}", comId);
                } else {
                    // CASE B: 자동 갱신이 꺼진 경우 (해지 예약 상태) -> 무료 요금제로 강등
                    log.info("[구독 만료 처리] 회사: {}, 자동갱신 꺼짐", comId);

                    Subscription freePlan = subscriptionRepository.findById(1L) // 1번: basic(free) 요금제
                            .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

                    sub.downgradeToFree(freePlan);

                    log.info("[무료 전환 완료] 회사: {}", comId);
                }
            } catch (Exception e) {
                // 결제 실패 시 에러 로그를 남기고 다음 대상으로 넘어감
                log.error("[결제 실패] 회사: {}, 사유: {}", comId, e.getMessage());
                // TODO: 여기에 결제 실패 알림(이메일 등) 로직을 추가
            }
        }

        log.info("[스케줄러 종료] 총 {}건 처리 완료", targets.size());
    }
}
