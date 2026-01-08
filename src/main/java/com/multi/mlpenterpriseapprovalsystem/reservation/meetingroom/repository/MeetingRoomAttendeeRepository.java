package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.repository;

import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoomAttendee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : MeetingRoomAttendeeRepository
 * @since : 2025-12-25 오후 9:44 목요일
 */


public interface MeetingRoomAttendeeRepository extends JpaRepository<MeetingRoomAttendee, Long> {
    List<MeetingRoomAttendee> findAllByMeetingRoomReservation_MeetingResvNo(Long meetingResvNo);
}
