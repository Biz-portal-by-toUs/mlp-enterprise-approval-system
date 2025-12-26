package com.multi.mlpenterpriseapprovalsystem.meeting.domain;

import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의 참석 부서 엔티티
 *
 * @author : 김승기
 * @filename : MeetingDept
 * @since : 2025. 12. 23. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "meeting_dept",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_meeting_dept", columnNames = {"meet_no", "dep_no"})
        }
)
public class MeetingDept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meet_dept_no")
    private Long meetDeptNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meet_no", referencedColumnName = "meet_no", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dep_no", referencedColumnName = "dep_no", nullable = false)
    private Department department;

    private MeetingDept(Meeting meeting, Department department) {
        this.meeting = meeting;
        this.department = department;
    }

    public static MeetingDept create(Meeting meeting, Department department) {
        return new MeetingDept(meeting, department);
    }
}