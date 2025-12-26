package com.multi.mlpenterpriseapprovalsystem.document.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentFormCategory;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Table(name = "document")
@Builder
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

    @Column(columnDefinition = "json")
    private String content = "";

    @OneToMany( mappedBy = "document", fetch = FetchType.LAZY)
    private List<ApprovalLine> approvalLines;

    @Lob
    @Column(name = "cntt_html")
    private String cnttHtml = "";

    // 작성자 참조 (emp_id -> employee.emp_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", nullable = false)
    private Employee writer;

    @Lob
    private String aiSumm;

    @Column(nullable = false)
    private Boolean temp; // 임시 저장 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "docfo_no", nullable = false)
    private DocumentForm documentForm;

    // 문서 상태(상신전, 결재중, 최종승인, 반려)
    @Column(name = "doc_stat", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocStat docStat = DocStat.AW;

    // 상신일(
    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    /**
     * 문서 내용 수정 (임시저장 문서용)
     */
    public void update(String title, String content, String cnttHtml, String aiSumm, DocumentFormCategory category) {
        this.title = title;
        this.content = content;
        this.cnttHtml = cnttHtml;
        this.aiSumm = aiSumm;
        this.documentFormCategory = category;
    }


    // 최종승인
    // 문서코드(docId) 생성
    // 문서가 최종승인되어야 발급
    // 회사약어 최대3자리(comId) + 부서코드 최대3자리(depId) + 년도4자리 + 일련번호 4자리 = 최대 총 14자리
    // 현재는 가짜 데이터 넣어놔서 14자리 넘음
    public void finalize() {
        this.docStat = DocStat.FI;
    }

    /**
     * 반려
     */
    public void reject() {
        this.docStat = DocStat.RJ;
    }

    public void cancelSubmit() {
        this.submittedAt = null;
        this.temp = true;
        this.docStat = DocStat.US;
    }

    // Document 엔티티에 추가
    public void submit() {
        this.temp = false;
        this.submittedAt = LocalDateTime.now();
        this.docStat = DocStat.AW;
    }

    // 임시저장용
    public void saveAsTemp() {
        this.temp = true;
        this.submittedAt = null;
        this.docStat = DocStat.US; // 또는 별도 상태가 있다면 변경
    }

    // 최종승인 시 문서코드 발행
    public void finalize(String docId) {
        this.docId = docId;
        this.docStat = DocStat.FI;
    }

    // 임시저장여부, 문서상태는 직접 넣기
    public static Document toEntity(ReqDocumentDto dto,
                                    Company company,
                                    Employee writer,
                                    DocumentFormCategory category,
                                    DocumentForm form) {
        return Document.builder()
                .company(company)
                .documentFormCategory(category)
                .title(dto.getTitle())
                .content(dto.getContent())
                .cnttHtml(dto.getCnttHtml())
                .writer(writer)
                .aiSumm(dto.getAiSumm())
                .documentForm(form)
                .build();
    }

    // 임시저장여부, 문서상태는 직접 넣기
    public static Document toEntity(ReqDocumentDto dto,
                                    Company company,
                                    Employee writer,
                                    DocumentFormCategory category,
                                    DocumentForm form,
                                    String docId) {
        return Document.builder()
                .company(company)
                .docId(docId)
                .documentFormCategory(category)
                .title(dto.getTitle())
                .content(dto.getContent())
                .cnttHtml(dto.getCnttHtml())
                .writer(writer)
                .aiSumm(dto.getAiSumm())
                .documentForm(form)
                .build();
    }

}