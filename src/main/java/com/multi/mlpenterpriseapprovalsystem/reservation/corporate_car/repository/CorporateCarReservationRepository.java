package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.repository;

import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain.CorporateCarReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 법인 차량 예약 데이터 접근을 담당하는 Repository
 *
 * 법인 차량 예약 엔티티(MeetingRoomReservation)에 대한
 * 조회 기능을 제공하는 Spring Data JPA Repository이다.
 *
 * @author : 송현님
 * @filename : CorporateCarReservationRepository
 * @since : 2025-12-27 오후 10:37 토요일
 */

@Repository
public interface CorporateCarReservationRepository extends JpaRepository<CorporateCarReservation, Long> {

    List<CorporateCarReservation> findByCompany_ComIdAndIsDeletedFalse(String comId);

    @Query(""" 
            select count(r) > 0
            from CorporateCarReservation r
            where r.corporateCar.carNo = :carNo
            and r.startedAt < :endedAt
            and r.endedAt > :startedAt
            """)
    boolean existsOverlapping(@Param("carNo") Long carNo,
                              @Param("startedAt") LocalDateTime startedAt,
                              @Param("endedAt") LocalDateTime endedAt);


    List<CorporateCarReservation> findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(String comId, String empId, LocalDateTime from, LocalDateTime toExclusive);
}
