package com.multi.mlpenterpriseapprovalsystem.subscription.repository;

import com.multi.mlpenterpriseapprovalsystem.subscription.domain.CompanySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 회사요금제정보 테이블 관리 레포지토리
 *
 * @author : 이지헌
 * @filename : CompanySubscriptionRepository
 * @since : 26. 1. 2. 금요일
 */
public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, Long> {
    Optional<CompanySubscription> findByCompany_ComId(String comId);

    // 특정 날짜 범위(오늘)에 해당하는 결제 예정 데이터 조회
    List<CompanySubscription> findAllByNextBillingDateBetween(LocalDateTime start, LocalDateTime end);
}
