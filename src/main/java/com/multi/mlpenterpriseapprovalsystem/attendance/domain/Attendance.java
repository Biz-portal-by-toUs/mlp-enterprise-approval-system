package com.multi.mlpenterpriseapprovalsystem.attendance.domain;

import com.multi.mlpenterpriseapprovalsystem.attendance.enums.AtteType;
import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 근태 엔티티
 * 휴가, 출장만 관리
 * @author : 이지헌
 * @filename : Attendance
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "attendance")
public class Attendance extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long atteNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_no", referencedColumnName = "doc_no")
    private Document document; // 최근에 이 근태에 영향을 끼친 문서

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AtteType type; // V(휴가), B(출장)

    @Column(name = "day", nullable = false)
    private Integer day; // 일 수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delegate", referencedColumnName = "emp_id", nullable = true)
    private Employee delegate; // 대직자

    @Column(name = "start_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime startAt; // 시작일

    @Column(name = "end_at", nullable = false, columnDefinition = "datetime")
    private LocalDateTime endAt; // 종료일

    @OneToMany(mappedBy = "attendance")
    @OrderBy("submittedAt DESC") // 최신 문서가 위로 오도록 정렬
    private List<Document> documents = new ArrayList<>();

    public void updateRecentDocument(Document document) {
        this.document = document;
    }

    /**
     * 근태 날짜 수정
     */
    public void modifyDates(LocalDateTime newStartAt, LocalDateTime newEndAt, int newDays) {
        this.startAt = newStartAt;
        this.endAt = newEndAt;
        this.day = newDays;
    }

    /**
     * 대직자 수정
     */
    public void updateDelegate(Employee newDelegate) {
        this.delegate = newDelegate;
    }

}