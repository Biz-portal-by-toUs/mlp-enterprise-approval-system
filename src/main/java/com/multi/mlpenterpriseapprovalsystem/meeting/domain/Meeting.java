package com.multi.mlpenterpriseapprovalsystem.meeting.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 회의 엔티티
 *
 * @author : 김승기
 * @filename : Meeting
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meeting")
public class Meeting extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meet_no")
    private Long meetNo;

    @Column(nullable = false)
    private String title;

    @Lob
    private String sttText;

    @Lob
    private String aiText;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 공개/비공개 (true=공개 예시)
    @Column(name = "status", nullable = true)
    private Boolean status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", nullable = false)
    private Employee writer;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MeetingDept> meetingDepts = new ArrayList<>();

    public static Meeting create(@NotBlank String title,
                                 @NotNull LocalDateTime startedAt,
                                 Company company,
                                 Employee writer,
                                 @NotNull Boolean status) {
        Meeting meeting = new Meeting();
        meeting.title = title;
        meeting.startedAt = startedAt;
        meeting.company = company;
        meeting.writer = writer;
        meeting.status = status;

        // 기본값
        meeting.isDeleted = false;

        return meeting;
    }
    public void addDepartment(Department department) {
        this.meetingDepts.add(MeetingDept.create(this, department));
    }

    public void clearDepartments() {
        this.meetingDepts.clear();
    }

    public void updateBasic(String title, LocalDateTime startedAt, Boolean status, String sttText, String aiText) {
        this.title = title;
        this.startedAt = startedAt;
        this.status = status;
        if (sttText != null) this.sttText = sttText;
        if (aiText != null) this.aiText = aiText;
    }

    public void delete() {
        this.isDeleted = true;
    }
}