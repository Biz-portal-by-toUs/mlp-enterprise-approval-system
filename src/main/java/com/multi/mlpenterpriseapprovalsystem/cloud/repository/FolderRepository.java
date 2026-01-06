package com.multi.mlpenterpriseapprovalsystem.cloud.repository;

import com.multi.mlpenterpriseapprovalsystem.cloud.domain.Folder;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 폴더 엔티티(Folder)에 대한 JPA Repository
 *
 * @author : 송현님
 * @filename : FolderRepository
 * @since : 2025-12-29 오전 10:24 월요일
 */

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {


    Optional<Folder> findByFolderNoAndComId(Long folderNo, String comId);

    @Query("""
                select f from Folder f
                where f.comId = :comId
                  and ( (:parentId is null and f.parentId is null) or (f.parentId = :parentId) )
                  and f.scope = :scope
                  and f.depNo = :depNo
                order by f.folderName asc
            """)
    List<Folder> findDeptChildren(@Param("comId") String comId,
                                  @Param("parentId") Long parentId,
                                  @Param("depNo") Long depNo,
                                  @Param("scope") FolderScope scope);

    @Query("""
                select f from Folder f
                where f.comId = :comId
                  and ( (:parentId is null and f.parentId is null) or (f.parentId = :parentId) )
                  and f.scope = :scope
                  and f.ownerId = :ownerId
                order by f.folderName asc
            """)
    List<Folder> findPrvtChildren(@Param("comId") String comId,
                                  @Param("parentId") Long parentId,
                                  @Param("ownerId") String ownerId,
                                  @Param("scope") FolderScope scope);

    @Modifying
    @Query(value = """
            UPDATE folder
            SET deleted_at = NOW(),
                deleted_by = :actor,
                delete_batch_id = :batchId
            WHERE com_id = :comId
              AND (folder_no = :targetId OR path LIKE CONCAT(:prefix, '%'))
            """, nativeQuery = true)
    int softDeleteFolderTree(String comId, Long targetId, String prefix, String actor, String batchId);

    @Query(value = """
        SELECT 
            'FOLDER' AS type,
            f.folder_no AS id,
            f.folder_name AS name,
            f.parent_id AS parentFolderNo,
            f.scope AS scope,
            f.dep_no AS depNo,
            f.owner_id AS ownerId,
            f.deleted_by AS deletedBy,
            f.deleted_at AS deletedAt,
            f.delete_batch_id AS deleteBatchId,
            NULL AS size,
            NULL AS contentType
        FROM folder f
        WHERE f.com_id = :comId
          AND f.deleted_at IS NOT NULL
          AND (
                (:scope = 'DEPT' AND f.scope = 'DEPT' AND f.dep_no = :depNo)
             OR (:scope = 'PRVT' AND f.scope = 'PRVT' AND f.owner_id = :ownerId)
          )

        UNION ALL

        SELECT
            'FILE' AS type,
            a.attachment_id AS id,
            a.original_name AS name,
            a.entity_id AS parentFolderNo,
            f.scope AS scope,
            f.dep_no AS depNo,
            f.owner_id AS ownerId,
            a.deleted_by AS deletedBy,
            a.deleted_at AS deletedAt,
            a.delete_batch_id AS deleteBatchId,
            a.size AS size,
            a.content_type AS contentType
        FROM attachment a
        JOIN folder f ON f.folder_no = a.entity_id
        WHERE a.com_id = :comId
          AND a.domain = 'CLOUD'
          AND a.status = 'DELETED'
          AND (
                (:scope = 'DEPT' AND f.scope = 'DEPT' AND f.dep_no = :depNo)
             OR (:scope = 'PRVT' AND f.scope = 'PRVT' AND f.owner_id = :ownerId)
          )
        ORDER BY deletedAt DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> listTrashRaw(
            @Param("comId") String comId,
            @Param("scope") String scope,   // "DEPT" or "PRVT"
            @Param("depNo") Long depNo,
            @Param("ownerId") String ownerId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE folder
    SET deleted_at = NULL,
        deleted_by = NULL,
        delete_batch_id = NULL
    WHERE com_id = :comId
      AND delete_batch_id = :batchId
    """, nativeQuery = true)
    int restoreFolderBatch(@Param("comId") String comId, @Param("batchId") String batchId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE folder
    SET purged_at = NOW()
    WHERE com_id = :comId
      AND scope = 'PRVT'
      AND owner_id = :ownerId
      AND deleted_at IS NOT NULL
    """, nativeQuery = true)
    int purgeMyPrvtTrashFolders(@Param("comId") String comId, @Param("ownerId") String ownerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE folder
        SET purged_at = NOW()
        WHERE deleted_at IS NOT NULL
          AND deleted_at < DATE_SUB(NOW(), INTERVAL 30 DAY)
        """, nativeQuery = true)
    int purgeExpiredFolders();
}