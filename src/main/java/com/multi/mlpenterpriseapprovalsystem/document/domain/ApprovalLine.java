package com.multi.mlpenterpriseapprovalsystem.document.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 결재 라인 엔티티
 *
 * @author : 이지헌
 * @filename : ApprovalLine
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Table(name = "approval_line")
@Builder
public class ApprovalLine extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long apprlNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_no")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee approver; // 원결재자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    private int seq;

    @Enumerated(EnumType.STRING)
    private ApprStat apprStat;

    // 결재 시간
    private LocalDateTime endedAt;

    // 실제 결재자 여부
    @Column(name = "is_actual_appr", nullable = false)
    private Boolean isActualAppr;

    @Column(name = "rej_reason", columnDefinition = "text")
    private String rejReason;

    @Column(name = "is_delegate", nullable = false)
    Boolean isDelegate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_emp_id", referencedColumnName = "emp_id", nullable = true)
    Employee targetApprover; // 권한 위임자. 누구를 대신해서 내가 여기 있는가

    /**
     * 실제 결재자가 아닌 경우 상태만 변경 (같은 seq의 다른 결재자/대직자)
     */
    public void markAsNotActualApprover(ApprStat status, String rejReason) {
        this.apprStat = status;
        this.endedAt = LocalDateTime.now();
        this.isActualAppr = false;  // 실제 결재한 사람이 아님
        if (rejReason != null) {
            this.rejReason = rejReason;
        }
    }

    /**
     * 승인 처리
     */
    public void approve() {
        this.apprStat = ApprStat.A;
        this.endedAt = LocalDateTime.now();
        this.isActualAppr = true;
    }

    /**
     * 반려 처리
     */
    public void reject(String reason) {
        this.apprStat = ApprStat.R;
        this.endedAt = LocalDateTime.now();
        this.rejReason = reason;
        this.isActualAppr = true;
    }

    /**
     * 결재 진행중으로 변경
     */
    public void setInProgress() {
        this.apprStat = ApprStat.I;
    }

    public static ApprovalLine toEntity(Document document,
                                        Employee approver,
                                        Company company,
                                        int seq,
                                        ApprStat apprStat,
                                        boolean isDelegate,
                                        Employee targetApprover) {
        return ApprovalLine.builder()
                .document(document)
                .approver(approver)
                .company(company)
                .seq(seq)
                .apprStat(apprStat)
                .endedAt(null)
                .isActualAppr(false)
                .isDelegate(isDelegate)
                .rejReason(null)
                .targetApprover(targetApprover)
                .build();
    }
}