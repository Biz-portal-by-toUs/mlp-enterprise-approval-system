package com.multi.mlpenterpriseapprovalsystem.employee.domain;

import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_no")
    private Long empNo;

    @Column(name = "emp_id", nullable = false, unique = true, length = 7)
    private String empId; // Biz Key (UK)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;

    // [변경 반영] dep_id -> dep_no (PK 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_no", nullable = false)
    private Department department;

    // [변경 반영] pos_id -> pos_no (PK 참조)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pos_no", nullable = false)
    private Positions position;

    @Column(nullable = false)
    private String pwd;

    @Column(name = "emp_name", nullable = false, length = 20)
    private String empName;

    @Column(nullable = false, length = 50)
    private String email;

    @Column(nullable = false, length = 15)
    private String phone;

    @Column(name = "work_phone", nullable = false, length = 10)
    private String workPhone;

    @Column(nullable = false, length = 1)
    private String gen;

    @Column(name = "hire_date", nullable = false)
    private LocalDateTime hireDate;

    @Column(name = "ret_date")
    private LocalDateTime retDate; // Nullable

    @Column(nullable = false, length = 100)
    private String addr;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable=false, length=20)
    private RoleType role; // COM/SEC/THR/EMPLOYEE

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // 퇴사여부 // default = false

    @Column(nullable = false, length = 1)
    private String atte; // 근태(출장 = b , 휴가 = v, 근무 중 = c) // default = c

    @Column(name = "msg_stat", nullable = false, length = 1)
    private String msgStat; // 메시지 상태 ( 근무 중 = c, 회의 중 = m, 업무 집중 = d, 자리 비움 = x) // default = c

    // Self Reference (대직자 - emp_id 참조 유지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate", referencedColumnName = "emp_id")
    private Employee delegate;
}