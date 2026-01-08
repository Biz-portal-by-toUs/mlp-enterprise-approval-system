package com.multi.mlpenterpriseapprovalsystem.cloud.repository;

import com.multi.mlpenterpriseapprovalsystem.cloud.domain.CloudTrashLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * cloud_trash_log 테이블 접근을 위한 Spring Data JPA Repository
 *
 *  역할
 * 1) 관리자 휴지통 로그 조회(목록/하위 트리 조회)
 * 2) DELETE/RESTORE/PURGE 액션 발생 시 cloud_trash_log 테이블에 로그 적재(INSERT)
 *
 * @author : 송현님
 * @filename : CloudTrashLogRepository
 * @since : 2026-01-07 오후 2:35 수요일
 */
public interface CloudTrashLogRepository extends JpaRepository<CloudTrashLog, Long> {

    @Query(value = """
WITH root_folders AS (
    SELECT l.batch_id, f.path AS root_path
    FROM cloud_trash_log l
    JOIN folder f
      ON f.com_id = l.com_id
     AND f.folder_no = l.item_id
    WHERE l.com_id = :comId
      AND l.dep_no = :depNo
      AND UPPER(l.scope) = 'DEPT'
      AND l.action_at >= CONCAT(:from, ' 00:00:00')
      AND l.action_at <  DATE_ADD(CONCAT(:to, ' 00:00:00'), INTERVAL 1 DAY)
      AND (:q IS NULL OR :q = '' OR l.item_name LIKE CONCAT('%', :q, '%'))
      AND UPPER(l.item_type) = 'FOLDER'
)

SELECT
    l.action,
    l.action_at,
    l.actor,
    l.batch_id,
    l.item_type,
    l.item_id,
    l.item_name
FROM cloud_trash_log l
WHERE l.com_id = :comId
  AND l.dep_no = :depNo
  AND UPPER(l.scope) = 'DEPT'
  AND l.action_at >= CONCAT(:from, ' 00:00:00')
  AND l.action_at <  DATE_ADD(CONCAT(:to, ' 00:00:00'), INTERVAL 1 DAY)
  AND (:q IS NULL OR :q = '' OR l.item_name LIKE CONCAT('%', :q, '%'))
  AND (
      -- 1) 폴더: 같은 batch의 다른 루트 폴더 하위면(내부 폴더) 메인에서 제외
      (UPPER(l.item_type) = 'FOLDER'
       AND NOT EXISTS (
           SELECT 1
           FROM root_folders rf
           JOIN folder cf
             ON cf.com_id = l.com_id
            AND cf.folder_no = l.item_id
           WHERE rf.batch_id = l.batch_id
             AND cf.path LIKE CONCAT(rf.root_path, '/%')
       )
      )

      OR

      -- 2) 파일: 같은 batch의 루트 폴더 트리 안에 있으면(내부 파일) 메인에서 제외
      (UPPER(l.item_type) = 'FILE'
       AND NOT EXISTS (
           SELECT 1
           FROM root_folders rf
           JOIN attachment a
             ON a.com_id = l.com_id
            AND a.attachment_id = l.item_id
           JOIN folder pf
             ON pf.com_id = a.com_id
            AND pf.folder_no = a.entity_id
           WHERE rf.batch_id = l.batch_id
             AND (pf.path = rf.root_path OR pf.path LIKE CONCAT(rf.root_path, '/%'))
       )
      )
  )
ORDER BY l.action_at DESC, l.log_id DESC
LIMIT :limit OFFSET :offset
""", nativeQuery = true)
    List<Object[]> listDeptAdminLogsRaw(
            @Param("comId") String comId,
            @Param("depNo") Long depNo,
            @Param("from") String from,
            @Param("to") String to,
            @Param("q") String q,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    // ====== DELETE 로그 적재 (이미 attachment/folder가 soft-delete 된 뒤에 호출) ======

    @Modifying
    @Query(value = """
        INSERT INTO cloud_trash_log
            (com_id, scope, dep_no, action, action_at, actor, batch_id, item_type, item_id, item_name)
        SELECT
            a.com_id, f.scope, f.dep_no,
            'DELETE', a.deleted_at, a.deleted_by, a.delete_batch_id,
            'FILE', a.attachment_id, a.original_name
        FROM attachment a
        JOIN folder f ON f.folder_no = a.entity_id
        WHERE a.com_id = :comId
          AND a.domain = 'CLOUD'
          AND a.attachment_id = :attachmentId
          AND a.status = 'DELETED'
          AND f.scope = 'dept'
    """, nativeQuery = true)
    int insertDeleteLogForAttachment(
            @Param("comId") String comId,
            @Param("attachmentId") Long attachmentId
    );

    @Modifying
    @Query(value = """
        INSERT INTO cloud_trash_log
            (com_id, scope, dep_no, action, action_at, actor, batch_id, item_type, item_id, item_name)
        SELECT
            f.com_id, f.scope, f.dep_no,
            'DELETE', f.deleted_at, f.deleted_by, f.delete_batch_id,
            'FOLDER', f.folder_no, f.folder_name -- ⚠️ 실제 폴더명 컬럼으로 변경!
        FROM folder f
        WHERE f.com_id = :comId
          AND f.folder_no = :folderNo
          AND f.deleted_at IS NOT NULL
    """, nativeQuery = true)
    int insertDeleteLogForFolder(
            @Param("comId") String comId,
            @Param("folderNo") Long folderNo
    );

    // ====== RESTORE 로그 적재 (복구 UPDATE 전에 호출) ======
    @Modifying
    @Query(value = """
        INSERT INTO cloud_trash_log
            (com_id, scope, dep_no, action, action_at, actor, batch_id, item_type, item_id, item_name)
        SELECT
            a.com_id, f.scope, f.dep_no,
            'RESTORE', NOW(), :actor, :batchId,
            'FILE', a.attachment_id, a.original_name
        FROM attachment a
        JOIN folder f ON f.folder_no = a.entity_id
        WHERE a.com_id = :comId
          AND a.domain = 'CLOUD'
          AND a.status = 'DELETED'
          AND a.delete_batch_id = :batchId
          AND f.scope = 'dept'
          AND f.dep_no = :depNo
    """, nativeQuery = true)
    int insertRestoreLogsForDeptAttachmentsByBatch(
            @Param("comId") String comId,
            @Param("depNo") Long depNo,
            @Param("batchId") String batchId,
            @Param("actor") String actor
    );

    @Modifying
    @Query(value = """
        INSERT INTO cloud_trash_log
            (com_id, scope, dep_no, action, action_at, actor, batch_id, item_type, item_id, item_name)
        SELECT
            f.com_id, f.scope, f.dep_no,
            'RESTORE', NOW(), :actor, :batchId,
            'FOLDER', f.folder_no, f.folder_name -- ⚠️ 실제 폴더명 컬럼으로 변경!
        FROM folder f
        WHERE f.com_id = :comId
          AND f.deleted_at IS NOT NULL
          AND f.delete_batch_id = :batchId
          AND f.scope = 'dept'
          AND f.dep_no = :depNo
    """, nativeQuery = true)
    int insertRestoreLogsForDeptFoldersByBatch(
            @Param("comId") String comId,
            @Param("depNo") Long depNo,
            @Param("batchId") String batchId,
            @Param("actor") String actor
    );

    // ====== PURGE 로그 적재 (스케줄러 하드삭제 직전에 호출, actor는 'SYSTEM') ======
    @Modifying
    @Query(value = """
        INSERT INTO cloud_trash_log
            (com_id, scope, dep_no, action, action_at, actor, batch_id, item_type, item_id, item_name)
        SELECT
            a.com_id, f.scope, f.dep_no,
            'PURGE', NOW(), 'SYSTEM', a.delete_batch_id,
            'FILE', a.attachment_id, a.original_name
        FROM attachment a
        JOIN folder f ON f.folder_no = a.entity_id
        WHERE a.domain = 'CLOUD'
          AND a.status = 'DELETED'
          AND a.deleted_at < DATE_SUB(NOW(), INTERVAL :days DAY)
    """, nativeQuery = true)
    int insertPurgeLogsForExpiredAttachments(@Param("days") int days);

    @Modifying
    @Query(value = """
        INSERT INTO cloud_trash_log
            (com_id, scope, dep_no, action, action_at, actor, batch_id, item_type, item_id, item_name)
        SELECT
            f.com_id, f.scope, f.dep_no,
            'PURGE', NOW(), 'SYSTEM', f.delete_batch_id,
            'FOLDER', f.folder_no, f.folder_name -- ⚠️ 실제 폴더명 컬럼으로 변경!
        FROM folder f
        WHERE f.deleted_at < DATE_SUB(NOW(), INTERVAL :days DAY)
    """, nativeQuery = true)
    int insertPurgeLogsForExpiredFolders(@Param("days") int days);

    @Query(value = """
    WITH parent AS (
        SELECT f.path AS base_path
        FROM folder f
        WHERE f.com_id = :comId
          AND f.dep_no = :depNo
          AND UPPER(f.scope) = 'DEPT'
          AND f.folder_no = :folderNo
        LIMIT 1
    )
    SELECT
        NULL AS action,
        NULL AS actionAt,
        NULL AS actor,
        UPPER(l.item_type) AS itemType,
        l.item_id AS itemId,
        l.item_name AS itemName
    FROM cloud_trash_log l
    JOIN parent p ON 1=1

    -- 폴더 path 확인용
    LEFT JOIN folder cf
      ON cf.com_id = l.com_id
     AND cf.folder_no = l.item_id
     AND UPPER(l.item_type) = 'FOLDER'

    -- 파일의 부모폴더 path 확인용
    LEFT JOIN attachment a
      ON a.com_id = l.com_id
     AND a.attachment_id = l.item_id
     AND UPPER(l.item_type) = 'FILE'
    LEFT JOIN folder pf
      ON pf.com_id = a.com_id
     AND pf.folder_no = a.entity_id

    WHERE l.com_id = :comId
      AND l.dep_no = :depNo
      AND UPPER(l.scope) = 'DEPT'
      AND l.batch_id = :batchId

      -- 루트 폴더 자신은 children에서 제외(원하면 빼도 됨)
      AND NOT (UPPER(l.item_type)='FOLDER' AND l.item_id = :folderNo)

      AND (
        -- 하위 폴더
        (UPPER(l.item_type)='FOLDER'
          AND cf.path LIKE CONCAT(p.base_path, '/%')
        )
        OR
        -- 폴더 트리 내부의 파일(루트 포함)
        (UPPER(l.item_type)='FILE'
          AND (pf.path = p.base_path OR pf.path LIKE CONCAT(p.base_path, '/%'))
        )
      )
    ORDER BY
      CASE WHEN UPPER(l.item_type)='FOLDER' THEN 0 ELSE 1 END,
      l.item_name ASC
""", nativeQuery = true)
    List<Object[]> listDeptTrashChildrenRaw(
            @Param("comId") String comId,
            @Param("depNo") Long depNo,
            @Param("batchId") String batchId,
            @Param("folderNo") Long folderNo
    );

    @Modifying
    @Query(value = """
INSERT INTO cloud_trash_log
(com_id, scope, dep_no, action, action_at, actor, batch_id, item_type, item_id, item_name)
VALUES
(:comId, 'dept', :depNo, 'PURGE', NOW(), :actor, :batchId, 'FILE', :itemId, :itemName)
""", nativeQuery = true)
    int insertManualPurgeLogForDeptFile(
            @Param("comId") String comId,
            @Param("depNo") Long depNo,
            @Param("actor") String actor,
            @Param("batchId") String batchId,
            @Param("itemId") Long itemId,
            @Param("itemName") String itemName
    );



}

