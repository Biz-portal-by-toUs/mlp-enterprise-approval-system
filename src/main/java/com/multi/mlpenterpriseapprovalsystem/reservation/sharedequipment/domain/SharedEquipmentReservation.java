package com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공유 설비 예약 엔티티
 *
 * 하나의 공유 설비(Shared Equipment)에 대해
 * 특정 시간(startedAt ~ endedAt) 동안 예약된 정보를 관리한다.
 *
 * @author : 고송현
 * @filename : SharedEquipmentReservation
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "shared_equipment_reservation")
public class SharedEquipmentReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eqResvNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eq_no")
    private SharedEquipment sharedEquipment;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resv_emp", referencedColumnName = "emp_id")
    private Employee resvEmp;

    private String purp;

    private Boolean isDeleted;

    @Builder
    public SharedEquipmentReservation(
            Company company,
            SharedEquipment sharedEquipment,
            Employee resvEmp,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String purp
    ) {
        this.company = company;
        this.sharedEquipment = sharedEquipment;
        this.resvEmp = resvEmp;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.purp = purp;
        this.isDeleted = false;
    }
}
