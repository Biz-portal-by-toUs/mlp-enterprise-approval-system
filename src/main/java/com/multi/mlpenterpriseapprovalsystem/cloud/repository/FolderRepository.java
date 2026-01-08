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
      and f.deletedAt is null        
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
      and f.deletedAt is null          
    order by f.folderName asc
""")
    List<Folder> findPrvtChildren(@Param("comId") String comId,
                                  @Param("parentId") Long parentId,
                                  @Param("ownerId") String ownerId,
                                  @Param("scope") FolderScope scope);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    UPDATE folder
    SET deleted_at = NOW(),
        deleted_by = :actor,
        delete_batch_id = :batchId
    WHERE com_id = :comId
      AND deleted_at IS NULL
      AND (
          folder_no = :folderNo
          OR path = :basePath
          OR path LIKE CONCAT(:basePath, '/%')
      )
""", nativeQuery = true)
    int softDeleteFolderTree(
            @Param("comId") String comId,
            @Param("folderNo") Long folderNo,
            @Param("basePath") String basePath,
            @Param("actor") String actor,
            @Param("batchId") String batchId
    );

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
            LEFT JOIN folder p
              ON p.folder_no = f.parent_id
             AND p.com_id = f.com_id
            WHERE f.com_id = :comId
              AND f.deleted_at IS NOT NULL
              AND (
                    (UPPER(:scope) = 'DEPT' AND f.scope = 'DEPT' AND f.dep_no = :depNo)
                 OR (UPPER(:scope) = 'PRVT' AND f.scope = 'PRVT' AND f.owner_id = :ownerId)
              )
              AND (f.parent_id IS NULL OR p.deleted_at IS NULL)

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
                    (UPPER(:scope) = 'DEPT' AND f.scope = 'DEPT' AND f.dep_no = :depNo)
                 OR (UPPER(:scope) = 'PRVT' AND f.scope = 'PRVT' AND f.owner_id = :ownerId)
              )
              AND f.deleted_at IS NULL

            ORDER BY deletedAt DESC
            LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> listTrashRaw(
            @Param("comId") String comId,
            @Param("scope") String scope,   // "DEPT"/"PRVT" 권장. "dept"/"prvt"도 들어오면 동작하도록 UPPER 처리됨.
            @Param("depNo") Long depNo,
            @Param("ownerId") String ownerId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );


    @Query(value = """
                SELECT f.folder_no
                FROM folder f
                WHERE f.com_id = :comId
                  AND f.scope = 'prvt'
                  AND f.owner_id = :ownerId
                  AND f.deleted_at IS NOT NULL
                ORDER BY LENGTH(f.path) DESC
            """, nativeQuery = true)
    List<Long> findMyPrvtDeletedFolderNos(@Param("comId") String comId,
                                          @Param("ownerId") String ownerId);

    @Modifying
    @Query(value = """
                DELETE FROM folder
                WHERE com_id = :comId
                  AND folder_no IN (:folderNos)
            """, nativeQuery = true)
    int hardDeleteFoldersByNos(@Param("comId") String comId,
                               @Param("folderNos") List<Long> folderNos);

    @Query(value = """
                SELECT f.folder_no
                FROM folder f
                WHERE f.deleted_at IS NOT NULL
                  AND f.deleted_at < DATE_SUB(NOW(), INTERVAL :days DAY)
                ORDER BY LENGTH(f.path) DESC
            """, nativeQuery = true)
    List<Long> findExpiredDeletedFolderNos(@Param("days") int days);

    @Modifying
    @Query(value = """
                DELETE FROM folder
                WHERE deleted_at IS NOT NULL
                  AND deleted_at < DATE_SUB(NOW(), INTERVAL :days DAY)
            """, nativeQuery = true)
    int hardDeleteExpiredDeletedFolders(@Param("days") int days);

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
                  AND f.scope = 'DEPT'
                  AND f.dep_no = :depNo
                  AND (:q IS NULL OR :q = '' OR f.folder_name LIKE CONCAT('%', :q, '%'))
            
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
                  AND f.scope = 'DEPT'
                  AND f.dep_no = :depNo
                  AND (:q IS NULL OR :q = '' OR a.original_name LIKE CONCAT('%', :q, '%'))
            
                ORDER BY deletedAt DESC
                LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> listAdminTrashLogRaw(
            @Param("comId") String comId,
            @Param("q") String q,
            @Param("depNo") Long depNo,
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
                  AND scope = 'prvt'
                  AND owner_id = :ownerId
            """, nativeQuery = true)
    int restoreFolderBatch(@Param("comId") String comId,
                           @Param("batchId") String batchId,
                           @Param("ownerId") String ownerId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                UPDATE folder
                SET deleted_at = NULL,
                    deleted_by = NULL,
                    delete_batch_id = NULL
                WHERE com_id = :comId
                  AND scope = 'dept'
                  AND dep_no = :depNo
                  AND delete_batch_id = :batchId
            """, nativeQuery = true)
    int restoreDeptFolderBatch(@Param("comId") String comId,
                               @Param("depNo") Long depNo,
                               @Param("batchId") String batchId);

    @Query(value = """
                SELECT f.path
                FROM folder f
                WHERE f.com_id = :comId
                  AND f.scope = 'prvt'
                  AND f.owner_id = :ownerId
                  AND f.deleted_at IS NOT NULL
                  AND f.folder_no = :folderNo
            """, nativeQuery = true)
    Optional<String> findPrvtDeletedFolderPath(String comId, String ownerId, Long folderNo);

    @Query(value = """
                SELECT f.folder_no
                FROM folder f
                WHERE f.com_id = :comId
                  AND f.scope = 'prvt'
                  AND f.owner_id = :ownerId
                  AND f.deleted_at IS NOT NULL
                  AND (f.folder_no = :rootId OR f.path LIKE CONCAT(:prefix, '%'))
                ORDER BY LENGTH(f.path) DESC
            """, nativeQuery = true)
    List<Long> findPrvtDeletedFolderNosInTree(String comId, String ownerId, Long rootId, String prefix);


}