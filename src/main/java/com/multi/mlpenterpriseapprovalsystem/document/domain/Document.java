package com.multi.mlpenterpriseapprovalsystem.document.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentFormCategory;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 문서 엔티티
 *
 * @author : 이지헌
 * @filename : Document
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "document")
public class Document extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docNo;

    // 회사 참조 (com_id -> company.com_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;

    // 문서 ID (UK 설정 권장)
    @Column(name = "doc_id", length = 14, unique = true)
    private String docId;

    // 카테고리 참조 (docfo_cat_no)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docfo_cat_no")
    private DocumentFormCategory documentFormCategory;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "json", nullable = false)
    private String content;


    @OneToMany( mappedBy = "document", fetch = FetchType.LAZY)
    private List<ApprovalLine> approvalLines;

    @Lob
    @Column(nullable = false)
    private String cnttHtml;

    // 작성자 참조 (emp_id -> employee.emp_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", nullable = false)
    private Employee writer;

    @Lob
    private String aiSumm;

    @Column(nullable = false)
    private Boolean temp; // 임시 저장 여부

    // 양식 참조 (docfo_no)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docfo_no", nullable = false)
    private DocumentForm documentForm;
}