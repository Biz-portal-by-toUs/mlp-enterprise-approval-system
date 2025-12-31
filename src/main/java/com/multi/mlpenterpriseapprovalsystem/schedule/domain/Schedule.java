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
    @Column(name="sch_no")
    private Long schNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    // [변경 반영] dep_no 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_no")
    private Department department;

    @Column(nullable = false)
    private String title;
    @Column
    private String content;

    @Column(name="start_at", nullable = false)
    private LocalDateTime startAt;
    @Column(name="ended_at", nullable = false)
    private LocalDateTime endedAt;

    @Column(name="all_day", nullable = false)
    private boolean allDay;

    @Column
    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reg_emp", referencedColumnName = "emp_id")
    private Employee register;

    private Schedule(Company company, Department department, Employee register,
                     String title, String content,
                     LocalDateTime startAt, LocalDateTime endedAt,
                     boolean allDay, String color) {
        this.company = company;
        this.department = department;
        this.register = register;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endedAt = endedAt;
        this.allDay = allDay;
        this.color = color;
    }

    public static Schedule create(Company company, Department department, Employee register,
                                  String title, String content,
                                  LocalDateTime startAt, LocalDateTime endedAt,
                                  boolean allDay, String color) {
        return new Schedule(company, department, register, title, content, startAt, endedAt, allDay, color);
    }
}