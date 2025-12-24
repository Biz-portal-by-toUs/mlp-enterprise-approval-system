package com.multi.mlpenterpriseapprovalsystem.document_form.form.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.*;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 문서 양식 도메인
 *
 * @author : 김승기
 * @filename : DocumentForm
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class) // created_at 자동 주입을 위해 필요
@Table(name = "document_form")
public class DocumentForm {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long docfoNo;

    // 회사 참조 (com_id -> company.com_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;

    // 작성자 참조 (writer_id -> employee.emp_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", referencedColumnName = "emp_id", nullable = false)
    private Employee writer;

    @Column(nullable = false, length = 100)
    private String docfoName;

    @Column(columnDefinition = "json", nullable = false)
    private String cnttJson;

    @Lob
    @Column(columnDefinition ="MEDIUMTEXT", nullable = false)
    private String cnttHtml;

    // 명세서에 created_at만 존재하므로 BaseEntity 상속 대신 직접 정의
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 1)
    @Enumerated(EnumType.STRING)
    private DocumentFormStats docfoStat; // T, P, R, A, D

    private String rejectReason;

    public static DocumentForm create(
            Company company,
            Employee writer,
            String docfoName,
            String cnttJson,
            String cnttHtml
    ) {
        DocumentForm f = new DocumentForm();
        f.company = company;
        f.writer = writer;
        f.docfoName = docfoName;
        f.cnttJson = cnttJson;
        f.cnttHtml = cnttHtml;
        f.docfoStat = DocumentFormStats.A; // 승인 로직 개발 후 T로 수정
        return f;
    }

    public void delete(){
        this.docfoStat=DocumentFormStats.D;
    }
}