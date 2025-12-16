package com.multi.mlpenterpriseapprovalsystem.reservation.reserve.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.reservation.register.domain.MeetingRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : MeetingRoomReservation
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meeting_room_reservation")
public class MeetingRoomReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long meetingResvNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "comId") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_no") private MeetingRoom meetingRoom;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resv_emp", referencedColumnName = "empId") private Employee resvEmp;
    private String purp;
    private Boolean isDeleted;
}