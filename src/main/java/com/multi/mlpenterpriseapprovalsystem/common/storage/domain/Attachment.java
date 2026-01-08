package com.multi.mlpenterpriseapprovalsystem.common.storage.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentFileType;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 권지영
 * @filename : Attachment
 * @since : 2025. 12. 23. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "attachment",
        indexes = {
                @Index(name = "idx_attach_ref", columnList = "comId, domain, entityId"),
                @Index(name = "idx_attach_ref_status", columnList = "comId, domain, entityId, status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_attach_order", columnNames = {"comId", "domain", "entityId", "displayOrder"}),
                @UniqueConstraint(name = "uk_object_key", columnNames = {"objectKey"})
        }
)
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    // 소속(tenant)
    @Column(name = "com_id", nullable = false, length = 3)
    private String comId;

    // 논리 참조: MAIL/NOTICE/BOARD/APPROVAL ...
    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false, length = 20)
    private AttachmentDomain domain;

    // 도메인 PK (mail_id / notice_id / post_id / appr_doc_id ...)
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    // 화면 표시 순서(1~5)
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder; // TINYINT UNSIGNED 대응 (Integer로 충분)

    // 파일 메타
    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 10)
    private AttachmentFileType fileType; // DOC/IMAGE/AUDIO

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "ext", length = 20)
    private String ext;

    // S3 위치
    @Column(name = "object_key", nullable = false, length = 255, unique = true)
    private String objectKey;

    @Column(name = "etag", length = 128)
    private String etag;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    // 소프트 삭제 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AttachmentStatus status = AttachmentStatus.ACTIVE;

    // 업로더 (emp_no 같은 값)
    @Column(name = "created_by")
    private String createdBy;


    // 생성용 팩토리(필요한 값만 받게)
    public static Attachment create(
            String comId,
            AttachmentDomain domain,
            Long entityId,
            Integer displayOrder,
            AttachmentFileType fileType,
            String originalName,
            String contentType,
            Long size,
            String ext,
            String objectKey,
            String etag,
            String checksumSha256,
            String createdBy
    ) {
        Attachment a = new Attachment();
        a.comId = comId;
        a.domain = domain;
        a.entityId = entityId;
        a.displayOrder = displayOrder;
        a.fileType = fileType;
        a.originalName = originalName;
        a.contentType = contentType;
        a.size = size;
        a.ext = ext;
        a.objectKey = objectKey;
        a.etag = etag;
        a.checksumSha256 = checksumSha256;
        a.createdBy = createdBy;
        a.status = AttachmentStatus.ACTIVE;
        return a;
    }

    // 소프트 삭제
    public void softDelete() {
        this.status = AttachmentStatus.DELETED;
    }

    public boolean isActive() {
        return this.status == AttachmentStatus.ACTIVE;
    }

    public void moveToEntityId(Long toFolderNo) {
        this.entityId = toFolderNo;
    }

    // 이동 시 목적지 폴더의 빈 displayOrder(1~5) 찾아서 재배정
    public void changeDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void rename(String originalName, String ext) {
        this.originalName = originalName;
        this.ext = ext;
    }

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 7)
    private String deletedBy;

    @Column(name = "delete_batch_id", length = 36)
    private String deleteBatchId;
}
