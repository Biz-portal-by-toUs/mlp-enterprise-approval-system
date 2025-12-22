package com.multi.mlpenterpriseapprovalsystem.subscription.repository;

import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 요금제 테이블 접근 repository
 *
 * @author : 이지헌
 * @filename : SubscriptionRepository
 * @since : 25. 12. 16. 화요일
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {


}
