package com.multi.mlpenterpriseapprovalsystem.document_form.form.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : DocumentFormCategory
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "document_form_category")
public class DocumentFormCategory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docfoCatNo;

    // 회사 참조 (com_id -> company.com_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId", nullable = false)
    private Company company;

    @Column(nullable = false, length = 100)
    private String name;

    // 문서 양식 참조 (docfo_no -> document_form.docfo_no)
    // *주의: 카테고리가 양식의 하위 개념으로 설계된 명세서를 따랐습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docfo_no", nullable = false)
    private DocumentForm documentForm;
}