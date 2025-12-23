package com.multi.mlpenterpriseapprovalsystem.meeting.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의 참석자 엔티티
 *
 * @author : 김승기
 * @filename : MeetingEmp
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "meeting_emp",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meeting_emp", columnNames = {"meet_no", "emp_id"})
        }
)
public class MeetingEmp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long meempNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meet_no", referencedColumnName = "meet_no", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", nullable = false)
    private Employee employee;

    private MeetingEmp(Meeting meeting, Employee employee, Company company) {
        this.meeting = meeting;
        this.employee = employee;
    }

    public static MeetingEmp create(Meeting meeting, Employee employee) {
        return new MeetingEmp(meeting, employee, meeting.getCompany());
    }

    public static MeetingEmp create(Meeting meeting, Employee employee, Company company) {
        return new MeetingEmp(meeting, employee, company);
    }
}