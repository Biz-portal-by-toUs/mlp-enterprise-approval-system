package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.domain.MeetingRoom;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 법인 차량 예약 엔티티
 *
 * 하나의 법인 차량(Corporate Car)에 대해
 * 특정 시간(startedAt ~ endedAt) 동안 예약된 정보를 관리한다.
 *
 * @author : 고송현
 * @filename : CorporateCarReservation
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "corporate_car_reservation")
public class CorporateCarReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long carResvNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_no")
    private CorporateCar corporateCar;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resv_emp", referencedColumnName = "emp_id")
    private Employee resvEmp;

    private String purp;

    private Boolean isDeleted;

    @Builder
    public CorporateCarReservation(
            Company company,
            CorporateCar corporateCar,
            Employee resvEmp,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String purp
    ) {
        this.company = company;
        this.corporateCar = corporateCar;
        this.resvEmp = resvEmp;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.purp = purp;
        this.isDeleted = false;
    }
}