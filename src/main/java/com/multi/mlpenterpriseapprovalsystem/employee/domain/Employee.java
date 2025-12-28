package com.multi.mlpenterpriseapprovalsystem.employee.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 사원 엔티티
 *
 * @author : 김승기
 * @filename : Employee
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "employee")
public class Employee extends BaseEntity {
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
    private Positions positions;

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
    private LocalDate hireDate;

    @Column(name = "ret_date")
    private LocalDate retDate; // Nullable

    @Column(nullable = false, length = 100)
    private String addr;

    @Enumerated(EnumType.STRING)
    @Column(name="role", nullable=false, length=20)
    private RoleType role; // COM/SEC/THR/EMPLOYEE

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false; // 퇴사여부 // default = false

    @Column(nullable = false, length = 1)
    private String atte; // 근태(출장 = B , 휴가 = V, 출근 = C) // default = C

    @Column(name = "msg_stat", nullable = false, length = 1)

    private String msgStat ; // 메시지 상태 ( 근무 중 = C, 회의 중 = M, 업무 집중 = D, 자리 비움 = X, 출근 안함 = H) // default = H

    // Self Reference (대직자 - emp_id 참조 유지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate", referencedColumnName = "emp_id")
    private Employee delegate;

    @Column(name = "object_key", nullable = true, length = 255, unique = true)
    private String objectKey;


    @Column(name = "birth", nullable = false)
    private LocalDate birth;

    /**
     * 사원 생성 (등록용)
     * - empId: comId + 4자리 일련번호 (ex. CAA0001)
     * - pwd: 반드시 인코딩된 값 전달
     */
    public static Employee create(
            String empId,
            Company company,
            Department department,
            Positions positions,
            String encodedPwd,
            String empName,
            String email,
            String phone,
            String workPhone,
            String gen,
            LocalDate hireDate,
            RoleType role,
            String addr,
            String objectKey,
            LocalDate birth
    ) {
        Employee e = new Employee();
        e.empId = empId;
        e.company = company;
        e.department = department;
        e.positions = positions;
        e.pwd = encodedPwd;
        e.empName = empName;
        e.email = email;
        e.phone = phone;
        e.workPhone = workPhone;
        e.gen = gen;
        e.hireDate = hireDate;
        e.role = role;
        e.addr = addr;
        e.objectKey = objectKey;
        e.birth = birth;

        // 기본값
        e.isDeleted = false;
        e.retDate = null;
        e.atte = "C";
        e.msgStat = "H";

        return e;
    }


    @PrePersist
    public void prePersist() {
        if (msgStat == null) msgStat = "H";
        if (atte == null) atte = "C";
        if (isDeleted == null) isDeleted = false;
    } // 사원 상태 기본 출근 전(로그인 시 근무 중), 근태 기본 출근, 삭제 여부 기본 false 세팅

    public void updateObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public void updateAdminInfo(Department dep, Positions pos,
                                String email, String phone, String workPhone, String addr,
                                String gen, RoleType role) {
        this.department = dep;
        this.positions = pos;
        this.email = email;
        this.phone = phone;
        this.workPhone = workPhone;
        this.addr = addr;
        this.gen = gen;
        this.role = role;
    }
}