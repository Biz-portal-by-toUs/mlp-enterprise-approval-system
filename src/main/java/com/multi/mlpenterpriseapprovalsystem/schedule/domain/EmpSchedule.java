package com.multi.mlpenterpriseapprovalsystem.schedule.domain;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 개인 일정 엔티티
 *
 * @author : 김승기
 * @filename : EmpSchedule
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "emp_schedule")
public class EmpSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="sch_no")
    private Long schNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;

    @Column(nullable = false)
    private String title;
    @Column
    private String content;

    @Column(name="start_at", nullable = false)
    private LocalDateTime startAt;
    @Column(name="ended_at", nullable = false)
    private LocalDateTime endedAt;

    private String color;

    private EmpSchedule(Employee employee, String title, String content,
                        LocalDateTime startAt, LocalDateTime endedAt, String color) {
        this.employee = employee;
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endedAt = endedAt;
        this.color = color;
    }

    public static EmpSchedule create(Employee employee, String title, String content,
                                     LocalDateTime startAt, LocalDateTime endedAt, String color) {
        return new EmpSchedule(employee, title, content, startAt, endedAt, color);
    }
}