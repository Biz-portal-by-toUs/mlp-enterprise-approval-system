package com.multi.mlpenterpriseapprovalsystem.document_form.attachment.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : AttachBox
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "attach_box")
public class AttachBox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attachNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id")
    private Company company;

    private String uploader;
    private String title;
    private String dscp;
    private String path;
    private Long size;
}