package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository;

import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회의실 예약 데이터 접근을 담당하는 Repository
 *
 * 회의실 예약 엔티티(MeetingRoomReservation)에 대한
 * 조회 기능을 제공하는 Spring Data JPA Repository이다.
 *
 * @author : 송현님
 * @filename : MeetingRoomReservationRepository
 * @since : 2025-12-22 오후 3:33 월요일
 */

@Repository
public interface MeetingRoomReservationRepository extends JpaRepository<MeetingRoomReservation, Long> {

    List<MeetingRoomReservation> findByCompany_ComIdAndIsDeletedFalse(String comId);

    @Query(""" 
            select count(r) > 0
            from MeetingRoomReservation r
            where r.meetingRoom.roomNo = :roomNo
            and r.startedAt < :endedAt
            and r.endedAt > :startedAt
            """)
    boolean existsOverlapping(@Param("roomNo") Long roomNo,
                              @Param("startedAt") LocalDateTime startedAt,
                              @Param("endedAt") LocalDateTime endedAt);

    List<MeetingRoomReservation> findAllByCompany_ComIdAndResvEmp_EmpIdAndStartedAtBetween(String comId, String empId, LocalDateTime from, LocalDateTime toExclusive);
}
