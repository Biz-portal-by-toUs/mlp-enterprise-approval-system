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
    List<PaymentMethod> findByCompanyAndActive(Company company, boolean active);

    // 특정 ID의 카드가 '활성' 상태인지 확인하며 조회
    Optional<PaymentMethod> findByPaymNoAndActiveTrue(Long paymNo);

    // 카드 등록 시 '현재 사용 중인(active=true)' 카드 중 중복이 있는지 확인
    Optional<PaymentMethod> findByCompanyAndMaskAndActiveTrue(Company company, String mask);

    // 사용자의 카드 목록을 불러올 때 삭제되지 않은 카드만 조회
    List<PaymentMethod> findAllByCompanyAndActiveTrue(Company company);

    // 관리자 페이지 등에서 삭제된 내역까지 포함해서 볼 때만 기존 메서드 사용
    List<PaymentMethod> findAllByCompany(Company company);
}
