package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회의실 예약 엔티티
 *
 * 하나의 회의실(MeetingRoom)에 대해
 * 특정 시간(startedAt ~ endedAt) 동안 예약된 정보를 관리한다.
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
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private
    Long meetingResvNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_no")
    private MeetingRoom meetingRoom;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resv_emp", referencedColumnName = "emp_id")
    private Employee resvEmp;

    private String purp;

    private boolean isDeleted;

    @Builder
    public MeetingRoomReservation(
            Company company,
            MeetingRoom meetingRoom,
            Employee resvEmp,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String purp
    ) {
        this.company = company;
        this.meetingRoom = meetingRoom;
        this.resvEmp = resvEmp;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.purp = purp;
        this.isDeleted = false;
    }

    // 도메인 행위
    public void delete() {
        this.isDeleted = true;
    }
}