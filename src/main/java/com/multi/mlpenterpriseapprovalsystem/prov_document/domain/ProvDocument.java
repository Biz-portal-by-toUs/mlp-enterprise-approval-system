package com.multi.mlpenterpriseapprovalsystem.prov_document.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사내 규정 메타데이터
 *
 * @author : 김승기
 * @filename : ProvDocument
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "prov_document")
public class ProvDocument extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long provNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    @Column(name = "doc_title",length = 30)
    private String docTitle;
    @Column(name="description",columnDefinition = "text")
    private String description;
    @Column(name = "is_public")
    private Boolean isPublic;
    @Column(name = "file_name", nullable = true)
    private String fileName;
    @Column(name = "object_key")
    private String objectKey;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "chunk_cnt")
    private Integer chunkCnt;
    @Enumerated(EnumType.STRING)
    @Column(name = "proc_stat", nullable = false, length = 20)
    private ProvProcStat procStat;
    @Column(name = "error_msg", length = 20)
    private String errorMsg;



    public void markUploaded(String fileName, String objectKey, Long fileSize) {
        this.fileName = fileName;
        this.objectKey = objectKey;
        this.fileSize = fileSize;
        this.procStat = ProvProcStat.UPLOADED;
        this.errorMsg = null;
    }

    public void markProcessing() {
        this.procStat = ProvProcStat.PROCESSING;
        this.errorMsg = null;
    }

    public void markDone(Integer chunkCnt) {
        this.procStat = ProvProcStat.DONE;
        this.chunkCnt = chunkCnt;
        this.errorMsg = null;
    }

    public void markFailed(String errorMsg) {
        this.procStat = ProvProcStat.FAILED;
        this.errorMsg = errorMsg;
    }

    public static ProvDocument create(Company company, String docTitle, String description, Boolean isPublic,
                                      String fileName, Long fileSize) {

        ProvDocument d = new ProvDocument();
        d.company = company;
        d.docTitle = docTitle;
        d.description = description;
        d.isPublic = (isPublic != null ? isPublic : Boolean.TRUE);

        d.fileName = fileName;
        d.fileSize = fileSize;

        d.chunkCnt = 0;
        d.procStat = ProvProcStat.CREATED;
        return d;
    }


    public void updateMeta(String docTitle, String description, Boolean isPublic) {
        this.docTitle = docTitle;
        this.description = description;
        this.isPublic = isPublic;
    }

}

