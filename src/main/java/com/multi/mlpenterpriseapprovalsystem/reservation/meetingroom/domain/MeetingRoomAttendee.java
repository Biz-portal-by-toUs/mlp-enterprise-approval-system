package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의실 예약 참석자 엔티티
 *
 * 회의실 예약(MeetingRoomReservation)과 직원(Employee) 간의
 * 다대다(M:N) 관계를 풀기 위한 연결 테이블 역할을 한다.
 *
 * @author : 고송현
 * @filename : MeetingRoomAttendee
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meeting_room_attendee")
public class MeetingRoomAttendee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long atteNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_resv_no")
    private MeetingRoomReservation meetingRoomReservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;
}