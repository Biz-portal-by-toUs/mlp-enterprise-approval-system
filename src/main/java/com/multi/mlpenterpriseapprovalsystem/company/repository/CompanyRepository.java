package com.multi.mlpenterpriseapprovalsystem.company.repository;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Company db 접근 레포지토리
 * 
 * @filename    : CompanyRepository
 * @author      : 권지영
 * @since       : 2025. 12. 17. 수요일
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Company> findByComId(String comId);

    boolean existsByComId(String comId);
}
