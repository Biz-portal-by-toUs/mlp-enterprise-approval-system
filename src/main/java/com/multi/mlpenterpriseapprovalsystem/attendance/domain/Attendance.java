package com.multi.mlpenterpriseapprovalsystem.attendance.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
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
 * @filename : Attendance
 * @since : 2025. 12. 16. 화요일
 */
@Entity @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "attendance")
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long atteNo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "com_id", referencedColumnName = "com_id") private Company company;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "emp_id", referencedColumnName = "emp_id") private Employee employee;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doc_id", referencedColumnName = "doc_id") private Document document;
    private String type;
    private Integer day;
    private String delegate;
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}