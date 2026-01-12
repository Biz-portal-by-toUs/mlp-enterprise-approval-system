package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.domain;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.*;

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

    @Column(nullable = false)
    private boolean committed;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String uploader;
    private String title;
    private String dscp;
    private String path;
    private Long size;

    public static AttachBox create(
            Company company,
            String uploader,
            String title,
            String dscp,
            String path,
            Long size
    ) {
        AttachBox a = new AttachBox();
        a.company = company;
        a.uploader = uploader;
        a.title = title;
        a.dscp = dscp;
        a.path = path;
        a.size = size;
        return a;
    }

    public void commit() { this.committed = true; }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.committed = false;
    }

    public void updateSize(Long size){
        this.size = (size == null || size < 0) ? 0L : size;
    }
}