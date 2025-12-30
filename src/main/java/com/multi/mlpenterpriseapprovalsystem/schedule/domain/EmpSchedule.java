package com.multi.mlpenterpriseapprovalsystem.schedule.domain;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    private String title;
    private String content;

    @Column(name="start_at")
    private LocalDate startAt;
    @Column(name="end_at")
    private LocalDate endedAt;

    private String color;
}