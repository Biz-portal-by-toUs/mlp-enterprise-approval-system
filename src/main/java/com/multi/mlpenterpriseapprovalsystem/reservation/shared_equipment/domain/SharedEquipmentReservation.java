package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.domain;

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
 * @filename : SharedEquipmentReservation
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "shared_equipment_reservation")
public class SharedEquipmentReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long eqResvNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "eq_no") private SharedEquipment sharedEquipment;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resv_emp", referencedColumnName = "emp_id") private Employee resvEmp;
    private String purp;
    private Boolean isDeleted;
}
