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
import com.multi.mlpenterpriseapprovalsystem.common.storage.repository.AttachmentRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dept 폴더는 depNo가 필요합니다.");
        }

        Long parentId = dto.getParentId();
        if (parentId != null) {
            Folder parent = getFolder(comId, parentId);
            // dept 트리 안에서만 생성 가능
            if (parent.getScope() != FolderScope.DEPT || !depNo.equals(parent.getDepNo())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "부모 폴더 접근 권한이 없습니다.");
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
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "부모 폴더 접근 권한이 없습니다.");
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
       3) 공통 기능: rename / delete
       - rename/delete 정책은 “개인함만”으로 갈지,
         “dept는 부서원도 가능”으로 갈지 팀 기준 따라 달라서
         지금은 기존 로직(소유자만 가능) 유지
       ========================= */

    public ResFolderDto rename(CustomUser user, Long folderNo, ReqRenameDto dto) {
        authCheck(user);

        Folder folder = getFolder(user.getComId(), folderNo);
        String ownerId = user.getUsername();
        // 기존 로직 그대로: 소유자만 변경 가능
        if (!folder.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "폴더명 변경 권한이 없습니다.");
        }

        folder.rename(dto.getFolderName().trim());
        return toDto(folder);
    }

    public void delete(CustomUser user, Long folderNo) {
        authCheck(user);

        Folder folder = getFolder(user.getComId(), folderNo);
        String ownerId = user.getUsername();

        if (!folder.getOwnerId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "폴더 삭제 권한이 없습니다.");
        }

        folderRepository.delete(folder);
    }


    /* =========================
       4) 파일 이동 (드래그 앤 드롭)
       - S3는 그대로, DB의 entityId만 변경
       ========================= */

    @Transactional
    public void moveCloudAttachment(CustomUser user, Long attachmentId, Long toFolderNo) {
        authCheck(user);

        String comId = user.getComId();
        String ownerId = user.getUsername();
        Long depNo = resolveDepNo(user);

        Folder toFolder = getFolder(comId, toFolderNo);
        if (!canView(toFolder, ownerId, depNo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "이동 대상 폴더 권한이 없습니다.");
        }

        Attachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다."));

        if (!comId.equals(att.getComId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "회사 권한이 없습니다.");
        }
        if (!"CLOUD".equals(att.getDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLOUD 파일만 이동 가능합니다.");
        }

        Long fromFolderNo = att.getEntityId();
        Folder fromFolder = getFolder(comId, fromFolderNo);
        if (!canView(fromFolder, ownerId, depNo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "원본 폴더 권한이 없습니다.");
        }

        // 공유함 ↔ 개인함 이동 금지
        if (fromFolder.getScope() != toFolder.getScope()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공유함과 개인함 간 이동은 불가합니다.");
        }

        att.moveToEntityId(toFolderNo);
    }


    /* =========================
       helpers
       ========================= */

    private void authCheck(CustomUser user) {
        if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
    }

    private Folder getFolder(String comId, Long folderNo) {
        return folderRepository.findByFolderNoAndComId(folderNo, comId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더가 없습니다."));
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