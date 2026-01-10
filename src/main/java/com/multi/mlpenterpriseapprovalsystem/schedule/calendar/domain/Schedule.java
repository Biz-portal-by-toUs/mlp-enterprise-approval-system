package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope; // 추가
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "schedule")
public class Schedule extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="sch_no")
    private Long schNo;

    // 일정의 범위 (PERSONAL, DEPARTMENT, COMPANY)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CalendarScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_no")
    private Department department;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT") // 내용이 길 수 있으므로 TEXT 권장
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

    // 생성자 수정
    private Schedule(CalendarScope scope, Company company, Department department, Employee register,
                     String title, String content,
                     LocalDateTime startAt, LocalDateTime endedAt,
                     boolean allDay, String color) {
        this.scope = scope; // ✅ 추가
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

    // 정적 팩토리 메서드 수정
    public static Schedule create(CalendarScope scope, Company company, Department department, Employee register,
                                  String title, String content,
                                  LocalDateTime startAt, LocalDateTime endedAt,
                                  boolean allDay, String color) {
        return new Schedule(scope, company, department, register, title, content, startAt, endedAt, allDay, color);
    }

    public void update (String title, String content, LocalDateTime startAt, LocalDateTime endedAt,
                        boolean allDay) {
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endedAt = endedAt;
        this.allDay = allDay;
    }
}