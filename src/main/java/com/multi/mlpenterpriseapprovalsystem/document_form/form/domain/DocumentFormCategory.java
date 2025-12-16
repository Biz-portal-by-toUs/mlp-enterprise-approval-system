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

    @Column(nullable = false, length = 7)
    private String docfoCatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId")
    private Company company;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "com_id", referencedColumnName = "com_id", insertable = false, updatable = false),
            @JoinColumn(name = "docfo_id", referencedColumnName = "docfo_id")
    })
    private DocumentForm documentForm;
}