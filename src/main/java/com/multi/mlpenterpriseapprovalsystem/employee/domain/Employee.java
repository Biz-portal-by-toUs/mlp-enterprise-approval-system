package com.multi.mlpenterpriseapprovalsystem.employee.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Employee
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "employee")
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long empNo;

    @Column(nullable = false, unique = true, length = 7)
    private String empId; // Biz Key (UK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId")
    private Company company;

    // [변경 반영] dep_id -> dep_no (PK 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_no")
    private Department department;

    // [변경 반영] pos_id -> pos_no (PK 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_no")
    private Positions position;

    @Column(nullable = false)
    private String pwd;

    @Column(nullable = false, length = 20)
    private String empName;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(nullable = false, length = 10)
    private String workPhone;

    @Column(nullable = false, length = 1)
    private String gen;

    @Column(nullable = false)
    private LocalDateTime hireDate;

    private LocalDateTime retDate; // Nullable

    @Column(nullable = false, length = 100)
    private String addr;

    @Column(nullable = false)
    private Long roleNo;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false, length = 1)
    private String atte;

    @Column(nullable = false, length = 1)
    private String msgStat;

    // Self Reference (대직자 - emp_id 참조 유지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate", referencedColumnName = "empId")
    private Employee delegate;
}