package com.multi.mlpenterpriseapprovalsystem.payment.repository;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 임시 파일
 * 지영님이 merge하면 삭제 예정
 *
 * @author : 이지헌
 * @filename : CompanyRepository
 * @since : 25. 12. 17. 수요일
 */
public interface PaymentCompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByComId(String comId);
}
