package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.repository;

import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain.CorporateCarReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain.SharedEquipmentReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 공유 설비 예약 데이터 접근을 담당하는 Repository
 *
 * 공유 설비 예약 엔티티(MeetingRoomReservation)에 대한
 * 조회 기능을 제공하는 Spring Data JPA Repository이다.
 *
 * @author : 송현님
 * @filename : SharedEquipmentReservationRepository
 * @since : 2025-12-28 오후 4:45 일요일
 */
public interface SharedEquipmentReservationRepository extends JpaRepository<SharedEquipmentReservation, Long> {

    List<SharedEquipmentReservation> findByCompany_ComIdAndIsDeletedFalse(String comId);

    @Query(""" 
            select count(r) > 0
            from SharedEquipmentReservation r
            where r.sharedEquipment.eqNo = :eqNo
            and r.startedAt < :endedAt
            and r.endedAt > :startedAt
            """)
    boolean existsOverlapping(@Param("eqNo") Long eqNo,
                              @Param("startedAt") LocalDateTime startedAt,
                              @Param("endedAt") LocalDateTime endedAt);

    List<SharedEquipmentReservation> findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(String comId, String empId, LocalDateTime from, LocalDateTime toExclusive);
}
