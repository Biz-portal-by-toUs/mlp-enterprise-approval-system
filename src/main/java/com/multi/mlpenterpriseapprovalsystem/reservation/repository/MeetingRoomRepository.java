package com.multi.mlpenterpriseapprovalsystem.reservation.repository;

import com.multi.mlpenterpriseapprovalsystem.reservation.register.domain.MeetingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Please explain the class!!!
 * 회의실(MeetingRoom) 엔티티에 대한 데이터 접근을 담당하는 Repository.
 *
 * JPA를 통해 회의실 관련 데이터를 조회 및 관리한다.
 * @author : 송현님
 * @filename : MeetingRoomReservationRepository
 * @since : 2025-12-16 오후 2:38 화요일
 */
@Repository
public interface MeetingRoomRepository extends JpaRepository<MeetingRoom, Long> {

    // 회사별 회의실 목록
    List<MeetingRoom> findByCompany_ComId(String comId);

}
