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
    @JoinColumn(name = "com_id", referencedColumnName = "comId")
    private Company company;

    private String docTitle;
    private String description;
    private Boolean isPublic;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private Integer chunkCnt;
    private String procStat;
    private String errorMsg;
}