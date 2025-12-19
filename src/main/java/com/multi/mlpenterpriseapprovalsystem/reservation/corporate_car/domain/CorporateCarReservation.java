package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : CorporateCarReservation
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "corporate_car_reservation")
public class CorporateCarReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long carResvNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "car_no") private CorporateCar corporateCar;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resv_emp", referencedColumnName = "emp_id") private Employee resvEmp;
    private String purp;
    private Boolean isDeleted;
}