package com.multi.mlpenterpriseapprovalsystem.reservation.repository;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : CompanyRepository
 * @since : 2025-12-17 오후 2:18 수요일
 */

@Repository
public interface ReservationCompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByComId(String comId);

    boolean existsByComId(String comId);
}
