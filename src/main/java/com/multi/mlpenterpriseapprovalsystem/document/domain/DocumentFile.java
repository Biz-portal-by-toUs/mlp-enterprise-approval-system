package com.multi.mlpenterpriseapprovalsystem.document.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문서 첨부 파일 엔티티
 *
 * @author : 이지헌
 * @filename : DocumentFile
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "document_file")
public class DocumentFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docfiNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doc_no")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    private String docfiName;
    private String folderPath;
}