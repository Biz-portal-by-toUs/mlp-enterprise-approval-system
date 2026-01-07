package com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.repository;

import com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.domain.CorporateCar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 법인 차량(CorporateCar) 엔티티에 대한 데이터 접근을 담당하는 Repository.
 *
 * JPA를 통해 법인 차량 관련 데이터를 조회 및 관리한다.
 * @author : 송현님
 * @filename : CorporateCarRepository
 * @since : 2025-12-21 오전 10:35 일요일
 */
@Repository
public interface CorporateCarRepository extends JpaRepository<CorporateCar, Long> {

    // 회사별 법인 차량 목록
    Page<CorporateCar> findByCompany_ComId(String comId, Pageable pageable);

    boolean existsByCompany_ComIdAndPlateNo(String comId, String plateNo);

    boolean existsByCompany_ComIdAndPlateNoAndCarNoNot(String comId, String newPlateNo, Long carNo);
}
