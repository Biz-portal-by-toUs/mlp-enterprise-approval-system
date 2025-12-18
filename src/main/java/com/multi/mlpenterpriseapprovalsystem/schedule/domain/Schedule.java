package com.multi.mlpenterpriseapprovalsystem.schedule.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : Schedule
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule")
public class Schedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long schNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    // [변경 반영] dep_no 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_no")
    private Department department;

    private String title;
    private String content;
    private LocalDateTime startAt;
    private LocalDateTime endedAt;
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reg_emp", referencedColumnName = "emp_id")
    private Employee register;
}