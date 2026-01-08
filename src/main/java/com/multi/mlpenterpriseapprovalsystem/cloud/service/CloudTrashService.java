package com.multi.mlpenterpriseapprovalsystem.cloud.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.domain.Folder;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResAdminTrashLogDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ReqTrashPurgeSelected;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResAdminTrashLogSliceDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResTrashItemDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import com.multi.mlpenterpriseapprovalsystem.cloud.repository.CloudTrashLogRepository;
import com.multi.mlpenterpriseapprovalsystem.cloud.repository.FolderRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Cloud 휴지통(Trash) 기능 전반을 담당하는 서비스
 *
 *  담당 기능
 * 1) 휴지통 목록 조회 (개인함/공유함)
 * 2) 삭제 묶음(batchId) 단위 복구(RESTORE)
 * 3) 개인함 휴지통 비우기(전체/선택)
 * 4) 관리자 감사/액션 로그 조회 (슬라이스 페이징)
 * 5) (부서함) 특정 파일 수동 영구삭제(PURGE)
 *
 * @author : 송현님
 * @filename : CloudTrashService
 * @since : 2026-01-05 오전 12:31 월요일
 */

@Service
@RequiredArgsConstructor
@Transactional
public class CloudTrashService {
    private final FolderRepository folderRepository;
    private final AttachmentRepository attachmentRepository;
    private final EmployeeRepository employeeRepository;
    private final S3Client s3Client;
    private final CloudTrashLogRepository cloudTrashLogRepository;


    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.env}")
    private String env;


    @Transactional(readOnly = true)
    public List<ResTrashItemDto> listTrash(CustomUser user, FolderScope scope, int limit, int offset) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();
        Long depNo = resolveDepNo(user);

        if (scope == FolderScope.DEPT && depNo == null) return List.of();

       List<Object[]> rows = folderRepository.listTrashRaw(
               comId,
               scope.name().toLowerCase(),
               depNo,
               ownerId,
               limit,
               offset
       );

        return rows.stream().map(r -> new ResTrashItemDto(
                (String) r[0],                               // type
                ((Number) r[1]).longValue(),                 // id
                (String) r[2],                               // name
                r[3] == null ? null : ((Number) r[3]).longValue(), // parentFolderNo
                (String) r[4],                               // scope
                r[5] == null ? null : ((Number) r[5]).longValue(), // depNo
                (String) r[6],                               // ownerId
                (String) r[7],                               // deletedBy
                (LocalDateTime) r[8],                        // deletedAt
                (String) r[9],                               // deleteBatchId
                r[10] == null ? null : ((Number) r[10]).longValue(), // size
                (String) r[11]                               // contentType
        )).toList();
    }

    /**
     * ✅ 복구 정책
     * - 공유함(DEPT): "관리자만 복구" (너가 말한 정책)
     * - 개인함(PRVT): "본인 또는 관리자"로 둘 수도 있고,
     * 원하면 PRVT도 관리자만으로 바꿀 수 있음 (아래 주석 참고)
     */
    @Transactional
    public void restoreByBatch(CustomUser user, String batchId, FolderScope scope) {
        authCheck(user);

        String comId = user.getComId();
        String actor = user.getUsername();

        if (scope == FolderScope.DEPT) {
            // ✅ DEPT는 관리자만
            if (!isAdmin(user)) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);

            Long depNo = resolveDepNo(user);
            if (depNo == null) throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);

            // ✅ RESTORE 로그 적재 (복구 UPDATE 전에)
            cloudTrashLogRepository.insertRestoreLogsForDeptFoldersByBatch(comId, depNo, batchId, actor);
            cloudTrashLogRepository.insertRestoreLogsForDeptAttachmentsByBatch(comId, depNo, batchId, actor);

            int f = folderRepository.restoreDeptFolderBatch(comId, depNo, batchId);
            int a = attachmentRepository.restoreDeptAttachmentBatch(comId, depNo, batchId);

            if (f + a == 0) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);
            return;
        }

        // PRVT는 기존 정책 유지(본인만) - 원하면 관리자 허용도 가능
        int f = folderRepository.restoreFolderBatch(comId, batchId, actor);
        int a = attachmentRepository.restoreAttachmentBatch(comId, batchId, actor);

        if (f + a == 0) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);
    }


    /**
     * ✅ 개인함 휴지통 비우기(PRVT만)
     * - 공유함은 엔드포인트를 만들지 않으면 정책이 명확해짐
     */
    public void purgeMyPrvtTrash(CustomUser user) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();

        // 1) S3에서 삭제할 키 목록 확보
        List<String> keys = attachmentRepository.findMyPrvtDeletedObjectKeys(comId, ownerId);

        // 2) S3 먼저 삭제 (실패하면 DB는 삭제하지 않음)
        deleteObjectsFromS3(keys);

        // 3) DB에서 attachment 물리 삭제
        attachmentRepository.hardDeleteMyPrvtDeletedAttachments(comId, ownerId);

        // 4) DB에서 folder 물리 삭제 (자식 먼저)
        List<Long> folderNos = folderRepository.findMyPrvtDeletedFolderNos(comId, ownerId);
        if (!folderNos.isEmpty()) {
            folderRepository.hardDeleteFoldersByNos(comId, folderNos);
        }
    }

    private void authCheck(CustomUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    private Long resolveDepNo(CustomUser user) {
        String empId = user.getUsername();
        Employee emp = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        if (emp.getDepartment() == null || emp.getDepartment().getDepNo() == null) {
            throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        return emp.getDepartment().getDepNo();
    }

    private static final Set<String> ADMIN_AUTHORITIES = Set.of(
            "SYS_ADMIN", "COM_ADMIN", "SEC_ADMIN", "THR_ADMIN",
            "ROLE_SYS_ADMIN", "ROLE_COM_ADMIN", "ROLE_SEC_ADMIN", "ROLE_THR_ADMIN"
    );

    private boolean isAdmin(CustomUser user) {
        return user.getAuthorities() != null &&
                user.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .anyMatch(ADMIN_AUTHORITIES::contains);
    }

    private void deleteObjectsFromS3(List<String> keys) {
        if (keys == null || keys.isEmpty()) return;

        // 1000개씩 잘라서 DeleteObjects
        for (int i = 0; i < keys.size(); i += 1000) {
            List<String> chunk = keys.subList(i, Math.min(i + 1000, keys.size()));

            var objects = chunk.stream()
                    .map(k -> software.amazon.awssdk.services.s3.model.ObjectIdentifier.builder().key(k).build())
                    .toList();

            try {
                s3Client.deleteObjects(b -> b
                        .bucket(bucket)
                        .delete(d -> d.objects(objects))
                );
            } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
                // S3 실패 시: DB 삭제 안 하게 예외로 막음
                throw new CustomException(ErrorCode.FILE_DELETE_FAILED); // 너희 ErrorCode에 맞게
            }
        }
    }

    @Transactional(readOnly = true)
    public ResAdminTrashLogSliceDto listAdminActionLogs(
            CustomUser user,
            FolderScope scope,
            int limit,
            int offset,
            String from,
            String to,
            String q
    ) {
        authCheck(user);
        if (!isAdmin(user)) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);

        // 관리자 로그는 DEPT만
        if (scope == FolderScope.PRVT) {
            return new ResAdminTrashLogSliceDto(List.of(), false, 10, 0);
        }

        String comId = user.getComId();
        Long depNo = resolveDepNo(user);

        // ✅ 정책 방어: 10개씩
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        int safeOffset = Math.max(offset, 0);

        // ✅ 5페이지(50개) 밖이면 빈값
        if (safeOffset > 40) {
            return new ResAdminTrashLogSliceDto(List.of(), false, safeLimit, safeOffset);
        }

        // ✅ limit+1로 hasNext 판단
        List<Object[]> rows = cloudTrashLogRepository.listDeptAdminLogsRaw(
                comId, depNo, from, to, q, safeLimit + 1, safeOffset
        );

        boolean hasNext = rows.size() > safeLimit;
        if (hasNext) rows = rows.subList(0, safeLimit);

        List<ResAdminTrashLogDto> items = rows.stream().map(r -> new ResAdminTrashLogDto(
                (String) r[0],
                toLdt(r[1]),
                (String) r[2],
                (String) r[3],
                (String) r[4],
                ((Number) r[5]).longValue(),
                (String) r[6]
        )).toList();

        return new ResAdminTrashLogSliceDto(items, hasNext, safeLimit, safeOffset);
    }

    @Transactional
    public void purgeSelectedPrvt(CustomUser user, List<ReqTrashPurgeSelected.Item> items) {
        authCheck(user);

        if (items == null || items.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        String comId = user.getComId();
        String ownerId = user.getUsername();

        List<Long> fileIds = items.stream()
                .filter(it -> it != null && "FILE".equalsIgnoreCase(it.getType()) && it.getId() != null)
                .map(ReqTrashPurgeSelected.Item::getId)
                .distinct()
                .toList();

        List<Long> folderIds = items.stream()
                .filter(it -> it != null && "FOLDER".equalsIgnoreCase(it.getType()) && it.getId() != null)
                .map(ReqTrashPurgeSelected.Item::getId)
                .distinct()
                .toList();

        // 폴더 트리 확장(순서 보존+중복제거 권장)
        java.util.LinkedHashSet<Long> folderSet = new java.util.LinkedHashSet<>();
        for (Long rootId : folderIds) {
            String prefix = folderRepository.findPrvtDeletedFolderPath(comId, ownerId, rootId)
                    .orElseThrow(() -> new CustomException(ErrorCode.TRASH_PURGE_FORBIDDEN));

            List<Long> nos = folderRepository.findPrvtDeletedFolderNosInTree(comId, ownerId, rootId, prefix);
            folderSet.addAll(nos);
        }
        List<Long> folderNosToDelete = List.copyOf(folderSet);

        List<String> fileKeys = fileIds.isEmpty()
                ? List.of()
                : attachmentRepository.findMyPrvtDeletedObjectKeysByAttachmentIds(comId, ownerId, fileIds);

        List<String> folderKeys = folderNosToDelete.isEmpty()
                ? List.of()
                : attachmentRepository.findMyPrvtDeletedObjectKeysByFolderNos(comId, ownerId, folderNosToDelete);

        List<String> keys = java.util.stream.Stream.concat(fileKeys.stream(), folderKeys.stream())
                .filter(k -> k != null && !k.isBlank())
                .distinct()
                .toList();

        deleteObjectsFromS3(keys);

        if (!fileIds.isEmpty()) {
            attachmentRepository.hardDeleteMyPrvtDeletedAttachmentsByIds(comId, ownerId, fileIds);
        }
        if (!folderNosToDelete.isEmpty()) {
            attachmentRepository.hardDeleteMyPrvtDeletedAttachmentsByFolderNos(comId, ownerId, folderNosToDelete);
            folderRepository.hardDeleteFoldersByNos(comId, folderNosToDelete);
        }
    }

    @Transactional(readOnly = true)
    public List<ResAdminTrashLogDto> listAdminDeleteLog(
            CustomUser user,
            FolderScope scope,
            String from,
            String to,
            String q,
            int limit,
            int offset
    ) {
        authCheck(user);
        if (!isAdmin(user)) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);

        // 관리자 페이지는 DEPT 고정 권장
        if (scope == FolderScope.PRVT) return List.of();

        String comId = user.getComId();
        Long depNo = resolveDepNo(user);

        List<Object[]> rows = cloudTrashLogRepository.listDeptAdminLogsRaw(
                comId, depNo, from, to, q, limit, offset
        );

        return rows.stream().map(r -> new ResAdminTrashLogDto(
                (String) r[0],                 // action
                (java.time.LocalDateTime) r[1],// actionAt
                (String) r[2],                 // actor
                (String) r[3],                 // batchId
                (String) r[4],                 // type
                ((Number) r[5]).longValue(),   // id
                (String) r[6]                  // name
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<ResAdminTrashLogDto> listAdminAuditLog(
            CustomUser user,
            FolderScope scope,
            int limit,
            int offset,
            String q,
            String from,
            String to
    ) {
        authCheck(user);

        if (!isAdmin(user)) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);

        // 관리자 로그는 DEPT만
        if (scope == FolderScope.PRVT) return List.of();

        String comId = user.getComId();
        Long depNo = resolveDepNo(user);

        List<Object[]> rows = cloudTrashLogRepository.listDeptAdminLogsRaw(
                comId, depNo, from, to, q, limit, offset
        );

        return rows.stream().map(r -> new ResAdminTrashLogDto(
                s(r[0]),                           // action
                (LocalDateTime) r[1],              // actionAt
                s(r[2]),                           // actor
                s(r[3]),                           // batchId
                s(r[4]),                           // itemType
                nToLong(r[5]),                     // itemId  ✅ (Number/Object -> Long)
                s(r[6])                            // itemName
        )).toList();
    }

    private String s(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Long nToLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(o)); }
        catch (Exception e) { return null; }
    }

    private LocalDateTime toLdt(Object v) {
        if (v == null) return null;
        if (v instanceof java.time.LocalDateTime ldt) return ldt;
        if (v instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (v instanceof java.util.Date d) return new java.sql.Timestamp(d.getTime()).toLocalDateTime();
        return LocalDateTime.parse(String.valueOf(v)); // 최후의 수단(형식 맞을 때만)
    }

    @Transactional(readOnly = true)
    public List<ResAdminTrashLogDto> listAdminLogChildren(CustomUser user, String batchId, Long folderNo) {
        authCheck(user);
        if (!isAdmin(user)) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);

        String comId = user.getComId();
        Long depNo = resolveDepNo(user);

        // TODO: 여기서 folderNo 하위(트리) 폴더/파일을 batchId 기준으로 조회해서 내려줘야 함
        // 예: (folder 테이블 + attachment 테이블)에서 delete_batch_id = batchId 이고,
        //     folderNo 하위 경로/트리인 것만.
        return cloudTrashLogRepository.listDeptTrashChildrenRaw(comId, depNo, batchId, folderNo)
                .stream()
                .map(r -> new ResAdminTrashLogDto(
                        (String) r[0],                     // action(없으면 null로)
                        (LocalDateTime) r[1],              // actionAt(없으면 null)
                        (String) r[2],                     // actor(없으면 null)
                        batchId,                           // batchId
                        (String) r[3],                     // itemType (FOLDER/FILE)
                        ((Number) r[4]).longValue(),       // itemId
                        (String) r[5]                      // itemName
                )).toList();
    }

    @Transactional
    public void purgeDeptFile(CustomUser user, Long attachmentId) {
        authCheck(user);

        String comId = user.getComId();
        String actor = user.getUsername();
        Long depNo = resolveDepNo(user);

        // 1) 삭제 상태(DELETED) 파일 조회
        Attachment att = attachmentRepository
                .findByAttachmentIdAndComIdAndStatus(attachmentId, comId, AttachmentStatus.DELETED)
                .orElseThrow(() -> new CustomException(ErrorCode.TRASH_ITEM_NOT_FOUND));

        if (att.getDomain() != AttachmentDomain.CLOUD) {
            throw new CustomException(ErrorCode.ATTACHMENT_DOMAIN_INVALID);
        }

        // 2) 소속 폴더 확인(부서함 + 같은 부서)
        Folder folder = folderRepository.findByFolderNoAndComId(att.getEntityId(), comId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLDER_NOT_FOUND));

        if (folder.getScope() != FolderScope.DEPT || !depNo.equals(folder.getDepNo())) {
            throw new CustomException(ErrorCode.TRASH_PURGE_FORBIDDEN);
        }

        // 3) 권한: 관리자 OR 업로더(createdBy)
        boolean isUploader = actor != null && actor.equals(att.getCreatedBy());
        if (!isAdmin(user) && !isUploader) {
            throw new CustomException(ErrorCode.TRASH_PURGE_FORBIDDEN);
        }

        // 4) PURGE 로그
        cloudTrashLogRepository.insertManualPurgeLogForDeptFile(
                comId,
                depNo,
                actor,
                att.getDeleteBatchId(),
                att.getAttachmentId(),
                att.getOriginalName()
        );

        // 5) S3 삭제 → DB 삭제
        deleteObjectsFromS3(List.of(att.getObjectKey()));

        int deleted = attachmentRepository.hardDeleteDeletedCloudAttachmentById(comId, attachmentId);
        if (deleted == 0) throw new CustomException(ErrorCode.TRASH_ITEM_NOT_FOUND);
    }



}

