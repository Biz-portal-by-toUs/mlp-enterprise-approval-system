package com.multi.mlpenterpriseapprovalsystem.cloud.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.domain.Folder;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ReqFolderCreateDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ReqRenameDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResFolderDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import com.multi.mlpenterpriseapprovalsystem.cloud.repository.FolderRepository;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 클라우드 폴더(공유함 / 개인함) 비즈니스 로직을 담당하는 서비스
 *
 * 주요 정책
 * ------------------------------------------------------------------
 * 1) 공유함(DEPT)
 *    - 같은 회사 + 같은 부서(depNo)만 접근 가능
 *
 * 2) 개인함(PRVT)
 *    - 본인(ownerId)만 접근 가능
 *
 * 3) 폴더 이름 변경 / 삭제
 *    - 현재는 “소유자만” 가능 (팀 정책에 따라 변경 가능)
 *
 * 4) 파일 이동(드래그)
 *    - S3는 그대로 두고, 첨부의 entityId만 변경
 *    - 공유함 ↔ 개인함 간 이동 금지
 *
 * @author : 송현님
 * @filename : FolderService
 * @since : 2025-12-29 오전 10:25 월요일
 */

@Service
@RequiredArgsConstructor
@Transactional
public class FolderService {

    private final FolderRepository folderRepository;
    private final AttachmentRepository attachmentRepository;
    private final EmployeeRepository employeeRepository;

    /* =========================
       1) 부서 폴더 (공유함)
       ========================= */

    public ResFolderDto createDeptFolder(CustomUser user, ReqFolderCreateDto dto) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();
        Long depNo = resolveDepNo(user);
        if (depNo == null) {
            throw new CustomException(ErrorCode.FOLDER_DEPT_REQUIRED);
        }

        Long parentId = dto.getParentId();
        if (parentId != null) {
            Folder parent = getFolder(comId, parentId);

            // dept 트리 안에서만 생성 가능
            if (parent.getScope() != FolderScope.DEPT || !depNo.equals(parent.getDepNo())) {
                throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
            }
        }

        Folder saved = folderRepository.save(
                Folder.create(comId, depNo, parentId, dto.getFolderName().trim(), ownerId, FolderScope.DEPT)
        );

        saved.updatePath(buildPath(comId, parentId, saved.getFolderNo()));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ResFolderDto> listDept(CustomUser user, Long parentId) {
        authCheck(user);

        String comId = user.getComId();
        Long depNo = resolveDepNo(user);
        if (depNo == null) return List.of();

        return folderRepository.findDeptChildren(comId, parentId, depNo, FolderScope.DEPT)
                .stream().map(this::toDto).toList();
    }

    /* =========================
       2) 개인 폴더 (개인함)
       ========================= */

    public ResFolderDto createPrvtFolder(CustomUser user, ReqFolderCreateDto dto) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();

        Long parentId = dto.getParentId();
        if (parentId != null) {
            Folder parent = getFolder(comId, parentId);

            // prvt 트리 안에서만 생성 가능
            if (parent.getScope() != FolderScope.PRVT || !ownerId.equals(parent.getOwnerId())) {
                throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
            }
        }

        Folder saved = folderRepository.save(
                Folder.create(comId, null, parentId, dto.getFolderName().trim(), ownerId, FolderScope.PRVT)
        );

        saved.updatePath(buildPath(comId, parentId, saved.getFolderNo()));
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ResFolderDto> listPrvt(CustomUser user, Long parentId) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();

        return folderRepository.findPrvtChildren(comId, parentId, ownerId, FolderScope.PRVT)
                .stream().map(this::toDto).toList();
    }

    /* =========================
       3) 공통 기능: rename / delete(=트리 soft delete)
       ========================= */

    public ResFolderDto rename(CustomUser user, Long folderNo, ReqRenameDto dto) {
        authCheck(user);

        Folder folder = getFolder(user.getComId(), folderNo);
        String ownerId = user.getUsername();

        if (!folder.getOwnerId().equals(ownerId)) {
            throw new CustomException(ErrorCode.FOLDER_RENAME_FORBIDDEN);
        }

        folder.rename(dto.getFolderName().trim());
        return toDto(folder);
    }

    /**
     * ✅ 기존의 folderRepository.delete(folder) (하드삭제) 금지
     * ✅ 트리 + 파일 같이 soft delete
     */
    public void deleteFolderTree(CustomUser user, Long folderNo) {
        authCheck(user);

        String comId = user.getComId();
        String actorEmpId = user.getUsername(); // empId

        Folder folder = getFolder(comId, folderNo);

        // (현재 정책 유지: 소유자만 삭제 가능)
        if (!actorEmpId.equals(folder.getOwnerId())) {
            throw new CustomException(ErrorCode.FOLDER_DELETE_FORBIDDEN);
        }

        String batchId = UUID.randomUUID().toString();

        // ⭐ 중요: folder.path는 "/7/16" 형태(슬래시 포함, 자기 자신 포함)
        // 하위는 "/7/16/18" 이므로 prefix는 "/7/16/" 형태여야 함
        String prefix = ensureTrailingSlash(folder.getPath());

        folderRepository.softDeleteFolderTree(comId, folderNo, prefix, actorEmpId, batchId);
        attachmentRepository.softDeleteCloudFilesInTree(comId, folderNo, prefix, actorEmpId, batchId);
    }

    /* =========================
       4) 파일 이동 (드래그 앤 드롭)
       ========================= */

    public void moveCloudAttachment(CustomUser user, Long attachmentId, Long toFolderNo) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();
        Long depNo = resolveDepNo(user);

        Folder toFolder = getFolder(comId, toFolderNo);
        if (!canView(toFolder, ownerId, depNo)) {
            throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
        }

        Attachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTACHMENT_NOT_FOUND));

        if (!comId.equals(att.getComId())) {
            throw new CustomException(ErrorCode.ATTACHMENT_ACCESS_DENIED);
        }
        if (att.getDomain() != AttachmentDomain.CLOUD) {
            throw new CustomException(ErrorCode.ATTACHMENT_DOMAIN_INVALID);
        }

        Long fromFolderNo = att.getEntityId();
        Folder fromFolder = getFolder(comId, fromFolderNo);
        if (!canView(fromFolder, ownerId, depNo)) {
            throw new CustomException(ErrorCode.FOLDER_ACCESS_DENIED);
        }

        // 공유함 ↔ 개인함 이동 금지
        if (fromFolder.getScope() != toFolder.getScope()) {
            throw new CustomException(ErrorCode.CLOUD_MOVE_SCOPE_MISMATCH);
        }

        att.moveToEntityId(toFolderNo);
    }

    /* =========================
       helpers
       ========================= */

    private void authCheck(CustomUser user) {
        if (user == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    private Folder getFolder(String comId, Long folderNo) {
        return folderRepository.findByFolderNoAndComId(folderNo, comId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOLDER_NOT_FOUND));
    }

    private boolean canView(Folder f, String ownerId, Long depNo) {
        FolderScope scope = f.getScope();
        if (scope == null) return false;

        return switch (scope) {
            case DEPT -> depNo != null && depNo.equals(f.getDepNo());
            case PRVT -> ownerId != null && ownerId.equals(f.getOwnerId());
        };
    }

    private String buildPath(String comId, Long parentId, Long newFolderNo) {
        if (parentId == null) return "/" + newFolderNo;

        Folder parent = getFolder(comId, parentId);
        String parentPath = (parent.getPath() == null || parent.getPath().isBlank())
                ? ("/" + parentId) : parent.getPath();

        return parentPath + "/" + newFolderNo;
    }

    private String ensureTrailingSlash(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.endsWith("/") ? path : path + "/";
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

    private ResFolderDto toDto(Folder f) {
        return ResFolderDto.builder()
                .folderNo(f.getFolderNo())
                .parentId(f.getParentId())
                .folderName(f.getFolderName())
                .scope(f.getScope())
                .depNo(f.getDepNo())
                .ownerId(f.getOwnerId())
                .path(f.getPath())
                .createdAt(f.getCreatedAt())
                .build();
    }
}