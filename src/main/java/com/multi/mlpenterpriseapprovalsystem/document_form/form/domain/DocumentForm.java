package com.multi.mlpenterpriseapprovalsystem.document_form.form.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : DocumentForm
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "document_form", uniqueConstraints = @UniqueConstraint(columnNames = {"com_id", "docfo_id"}))
public class DocumentForm extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docfoNo;

    @Column(nullable = false, length = 6)
    private String docfoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId")
    private Company company;

    private String writerId;
    private String docfoName;
    @Column(columnDefinition = "json") private String cnttJson;
    @Lob private String cnttHtml;
    @Column(length = 1) private String docfoStat;
    private String rejectReason;
}