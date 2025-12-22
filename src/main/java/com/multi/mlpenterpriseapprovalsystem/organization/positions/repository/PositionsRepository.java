package com.multi.mlpenterpriseapprovalsystem.organization.positions.repository;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : 권지영
 * @filename : PositionsRepository
 * @since : 2025. 12. 22. 월요일
 */
public interface PositionsRepository extends JpaRepository<Positions, Long> {

    List<Positions> findAllByCompany_ComId(String comId);

    boolean existsByCompanyAndPosName(Company company, String posName);

    Optional<Positions> findByCompanyAndPosNo(Company company, Long posNo);

}
