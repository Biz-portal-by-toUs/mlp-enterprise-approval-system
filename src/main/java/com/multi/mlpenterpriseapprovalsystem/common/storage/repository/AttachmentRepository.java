package com.multi.mlpenterpriseapprovalsystem.common.storage.repository;

import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * attachment 관련 레포지토리
 *
 * @author : 권지영
 * @filename : AttachmentRepository
 * @since : 2025. 12. 24. 수요일
 */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    // ACTIVE 첨부(1~5 제한 체크용)
    long countByComIdAndDomainAndEntityIdAndStatus(
            String comId, AttachmentDomain domain, Long entityId, AttachmentStatus status
    );

    // 동시 업로드(complete 동시에 2번) 대비해서 같은 글의 첨부를 잠그고 읽기
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from Attachment a
            where a.comId = :comId
              and a.domain = :domain
              and a.entityId = :entityId
            """)
    List<Attachment> findAllByRefForUpdate(
            @Param("comId") String comId,
            @Param("domain") AttachmentDomain domain,
            @Param("entityId") Long entityId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Attachment a where a.attachmentId = :id")
    Optional<Attachment> findByIdForUpdate(@Param("id") Long id);


    List<Attachment> findByComIdAndDomainAndEntityIdAndStatusOrderByDisplayOrderAsc(
            String comId, AttachmentDomain domain, Long entityId, AttachmentStatus status
    );

    Optional<Attachment> findByAttachmentIdAndComIdAndStatus(
            Long attachmentId, String comId, AttachmentStatus status
    );

    @Modifying
    @Query(value = """
    UPDATE attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    SET a.status = 'DELETED',
        a.deleted_at = NOW(),
        a.deleted_by = :actor,
        a.delete_batch_id = :batchId
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.status = 'ACTIVE'
      AND (
          f.folder_no = :targetId
          OR f.path = :basePath
          OR f.path LIKE CONCAT(:basePath, '/%')
      )
""", nativeQuery = true)
    int softDeleteCloudFilesInTree(
            @Param("comId") String comId,
            @Param("targetId") Long targetId,
            @Param("basePath") String basePath,
            @Param("actor") String actor,
            @Param("batchId") String batchId
    );

    @Query(value = """
    SELECT a.object_key
    FROM attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.status = 'DELETED'
      AND f.scope = 'prvt'
      AND f.owner_id = :ownerId
""", nativeQuery = true)
    List<String> findMyPrvtDeletedObjectKeys(@Param("comId") String comId,
                                             @Param("ownerId") String ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE attachment
    SET status = 'DELETED',
        deleted_at = NOW(),
        deleted_by = :actor,
        delete_batch_id = :batchId
    WHERE com_id = :comId
      AND domain = 'CLOUD'
      AND attachment_id = :attachmentId
      AND status = 'ACTIVE'
""", nativeQuery = true)
    int softDeleteCloudAttachmentById(@Param("comId") String comId,
                                      @Param("attachmentId") Long attachmentId,
                                      @Param("actor") String actor,
                                      @Param("batchId") String batchId);

    @Modifying
    @Query(value = """
    DELETE a
    FROM attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.status = 'DELETED'
      AND f.scope = 'prvt'
      AND f.owner_id = :ownerId
""", nativeQuery = true)
    int hardDeleteMyPrvtDeletedAttachments(@Param("comId") String comId,
                                           @Param("ownerId") String ownerId);

    @Query(value = """
        SELECT a.object_key
        FROM attachment a
        WHERE a.domain = 'CLOUD'
          AND a.status = 'DELETED'
          AND a.deleted_at < DATE_SUB(NOW(), INTERVAL :days DAY)
    """, nativeQuery = true)
    List<String> findExpiredDeletedCloudObjectKeys(@Param("days") int days);

    @Modifying
    @Query(value = """
        DELETE FROM attachment
        WHERE domain = 'CLOUD'
          AND status = 'DELETED'
          AND deleted_at < DATE_SUB(NOW(), INTERVAL :days DAY)
    """, nativeQuery = true)
    int hardDeleteExpiredDeletedCloudAttachments(@Param("days") int days);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    SET a.status = 'ACTIVE',
        a.deleted_at = NULL,
        a.deleted_by = NULL,
        a.delete_batch_id = NULL
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.delete_batch_id = :batchId
      AND f.scope = 'prvt'
      AND f.owner_id = :ownerId
""", nativeQuery = true)
    int restoreAttachmentBatch(@Param("comId") String comId,
                               @Param("batchId") String batchId,
                               @Param("ownerId") String ownerId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    SET a.status = 'ACTIVE',
        a.deleted_at = NULL,
        a.deleted_by = NULL,
        a.delete_batch_id = NULL
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.delete_batch_id = :batchId
      AND f.scope = 'dept'
      AND f.dep_no = :depNo
""", nativeQuery = true)
    int restoreDeptAttachmentBatch(@Param("comId") String comId,
                                   @Param("depNo") Long depNo,
                                   @Param("batchId") String batchId);


    @Query(value = """
    SELECT a.object_key
    FROM attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.status = 'DELETED'
      AND a.attachment_id IN (:ids)
      AND f.scope = 'prvt'
      AND f.owner_id = :ownerId
""", nativeQuery = true)
    List<String> findMyPrvtDeletedObjectKeysByAttachmentIds(String comId, String ownerId, List<Long> ids);

    @Query(value = """
    SELECT a.object_key
    FROM attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.status = 'DELETED'
      AND a.entity_id IN (:folderNos)
      AND f.scope = 'prvt'
      AND f.owner_id = :ownerId
""", nativeQuery = true)
    List<String> findMyPrvtDeletedObjectKeysByFolderNos(String comId, String ownerId, List<Long> folderNos);

    @Modifying
    @Query(value = """
    DELETE a
    FROM attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.status = 'DELETED'
      AND a.attachment_id IN (:ids)
      AND f.scope = 'prvt'
      AND f.owner_id = :ownerId
""", nativeQuery = true)
    int hardDeleteMyPrvtDeletedAttachmentsByIds(String comId, String ownerId, List<Long> ids);

    @Modifying
    @Query(value = """
    DELETE a
    FROM attachment a
    JOIN folder f ON f.folder_no = a.entity_id
    WHERE a.com_id = :comId
      AND a.domain = 'CLOUD'
      AND a.status = 'DELETED'
      AND a.entity_id IN (:folderNos)
      AND f.scope = 'prvt'
      AND f.owner_id = :ownerId
""", nativeQuery = true)
    int hardDeleteMyPrvtDeletedAttachmentsByFolderNos(String comId, String ownerId, List<Long> folderNos);

    @Query("""
    select a from Attachment a
    where a.comId = :comId
      and a.domain = :domain
      and a.status = :status
      and a.attachmentId in :ids
""")
    List<Attachment> findAllForZip(
            @Param("comId") String comId,
            @Param("domain") AttachmentDomain domain,
            @Param("status") AttachmentStatus status,
            @Param("ids") List<Long> ids
    );

    boolean existsByComIdAndDomainAndEntityIdAndStatusAndOriginalName(
            String comId,
            AttachmentDomain domain,
            Long entityId,
            AttachmentStatus status,
            String originalName
    );

    // ✅ rename 시 "자기 자신" 제외
    boolean existsByComIdAndDomainAndEntityIdAndStatusAndOriginalNameAndAttachmentIdNot(
            String comId,
            AttachmentDomain domain,
            Long entityId,
            AttachmentStatus status,
            String originalName,
            Long attachmentId
    );

    @Modifying
    @Query(value = """
DELETE FROM attachment
WHERE com_id = :comId
  AND domain = 'CLOUD'
  AND attachment_id = :attachmentId
  AND status = 'DELETED'
""", nativeQuery = true)
    int hardDeleteDeletedCloudAttachmentById(
            @Param("comId") String comId,
            @Param("attachmentId") Long attachmentId
    );


}


