package com.multi.mlpenterpriseapprovalsystem.company.repository;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    boolean existsByBrn(String brn);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Company c where c.comId = :comId")
    Optional<Company> findByComIdForUpdate(@Param("comId") String comId);
}
