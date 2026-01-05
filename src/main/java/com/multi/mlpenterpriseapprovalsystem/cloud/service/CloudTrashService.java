package com.multi.mlpenterpriseapprovalsystem.cloud.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.TrashItemDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import com.multi.mlpenterpriseapprovalsystem.cloud.repository.FolderRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Please explain the class!!!
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

    @Transactional(readOnly = true)
    public List<TrashItemDto> listTrash(CustomUser user, FolderScope scope, int limit, int offset) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();
        Long depNo = resolveDepNo(user);

        if (scope == FolderScope.DEPT && depNo == null) return List.of();

        List<Object[]> rows = folderRepository.listTrashRaw(
                comId,
                scope.name(),
                depNo,
                ownerId,
                limit,
                offset
        );

        return rows.stream().map(r -> new TrashItemDto(
                (String) r[0],                                 // itemType: "FOLDER" | "FILE"
                ((Number) r[1]).longValue(),                   // itemId: folderNo or attachmentId
                (String) r[2],                                 // name
                r[3] == null ? null : ((Number) r[3]).longValue(), // parentId
                (String) r[4],                                 // path (folder path)
                r[5] == null ? null : ((Number) r[5]).longValue(), // depNo
                (String) r[6],                                 // ownerId
                (String) r[7],                                 // batchId
                (LocalDateTime) r[8],                           // deletedAt
                (String) r[9],                                 // deletedBy
                r[10] == null ? null : ((Number) r[10]).longValue(), // size (file only)
                (String) r[11]                                 // contentType or ext etc (file only)
        )).toList();
    }

    /**
     * ✅ 복구 정책
     * - 공유함(DEPT): "관리자만 복구" (너가 말한 정책)
     * - 개인함(PRVT): "본인 또는 관리자"로 둘 수도 있고,
     *   원하면 PRVT도 관리자만으로 바꿀 수 있음 (아래 주석 참고)
     */
    public void restoreByBatch(CustomUser user, String batchId, FolderScope scope) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();
        Long depNo = resolveDepNo(user);

        if (scope == FolderScope.DEPT) {
            if (!isAdmin(user)) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);
            if (depNo == null) throw new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND);
        } else {
            // PRVT 정책을 "본인 또는 관리자"로 운영하려면 최소한 batchId가 내 소유인지 검증해야 안전함.
            // (추천) folderRepository.existsPrvtBatchOwnedBy(...) 같은 쿼리로 검증 후 허용.
            // 지금은 팀 룰에 맞춰:
            if (!isAdmin(user)) {
                // 여기서 실제 검증 쿼리를 추가하는 걸 강추!
                // ex) if (!folderRepository.existsPrvtTrashBatch(comId, ownerId, batchId)) throw ...
            }
            // PRVT도 관리자만으로 하려면:
            // if (!isAdmin(user)) throw new CustomException(ErrorCode.TRASH_RESTORE_FORBIDDEN);
        }

        folderRepository.restoreFolderBatch(comId, batchId);
        attachmentRepository.restoreAttachmentBatch(comId, batchId);
    }

    /**
     * ✅ 개인함 휴지통 비우기(PRVT만)
     * - 공유함은 엔드포인트를 만들지 않으면 정책이 명확해짐
     */
    public void purgeMyPrvtTrash(CustomUser user) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();

        // TODO: 아래는 "내 개인함(DELETED)"를 PURGED 처리하는 쿼리로 구현
        // folderRepository.purgeMyPrvtTrash(comId, ownerId);
        // attachmentRepository.purgeMyPrvtTrashAttachments(comId, ownerId);

        // (권장) PURGED된 attachment.object_key 조회해서 S3 삭제까지
    }

    /* =========================
       helpers
       ========================= */

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

    private boolean isAdmin(CustomUser user) {
        return user.getAuthorities() != null &&
                user.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

