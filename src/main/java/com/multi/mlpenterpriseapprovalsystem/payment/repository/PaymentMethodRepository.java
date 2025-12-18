package com.multi.mlpenterpriseapprovalsystem.payment.repository;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 결제 수단 테이블 접근용 repository
 *
 * @author : 이지헌
 * @filename : PaymentMethodRepository
 * @since : 25. 12. 17. 수요일
 */
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    // 특정 회사와 빌링키로 결제 수단 조회 (중복 확인)
    Optional<PaymentMethod> findByCompanyAndBillingKey(Company company, String billingKey);

    Optional<PaymentMethod> findByCompanyAndMask(Company company, String mask);

    // 나중에 스케줄러에서 사용: 특정 회사의 활성화된 결제 수단 조회
    Optional<List<PaymentMethod>> findByCompanyAndActive(Company company, boolean active);
}
