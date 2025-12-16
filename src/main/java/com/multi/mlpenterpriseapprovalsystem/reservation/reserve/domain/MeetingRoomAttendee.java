package com.multi.mlpenterpriseapprovalsystem.reservation.reserve.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : MeetingRoomAttendee
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meeting_room_attendee")
public class MeetingRoomAttendee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long atteNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "comId") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "meeting_resv_no") private MeetingRoomReservation meetingRoomReservation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "emp_id", referencedColumnName = "empId") private Employee employee;
}