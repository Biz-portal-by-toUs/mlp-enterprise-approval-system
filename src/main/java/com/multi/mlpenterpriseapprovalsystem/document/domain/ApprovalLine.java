package com.multi.mlpenterpriseapprovalsystem.document.domain;

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
public class ApprovalLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long apprlNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_no")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id")
    private Employee approver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    private int seq;

    @Enumerated(EnumType.STRING)
    private ApprStat apprStat;

    // 결재 시간이므로 직접 시간을 넣어줘야함
    private LocalDateTime endedAt;

    // 실제 결재자 여부
    @Column(name = "is_actual_appr", nullable = false)
    private Boolean isActualAppr;

    @Column(name = "rej_reason", columnDefinition = "text")
    private String rejReason;

    @Column(name = "is_delegate", nullable = false)
    Boolean isDelegate = false;

    public static ApprovalLine toEntity(Document document,
                                        Employee approver,
                                        Company company,
                                        int seq,
                                        ApprStat apprStat,
                                        boolean isDelegate) {
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
                .build();
    }
}