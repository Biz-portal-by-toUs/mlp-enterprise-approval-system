package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ReqFolderCreateDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ReqRenameDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResFolderDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.service.FolderService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.AttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클라우드 폴더(공유함 / 개인함) 관련 REST 컨트롤러
 * 
 * @filename    : FolderController
 * @author      : 송현님
 * @since       : 2025-12-29 오후 4:53 월요일
 */

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final AttachmentService attachmentService;

    @PostMapping("/dept")
    public ResponseEntity<ResponseDto<ResFolderDto>> createDept(
            @RequestBody @Valid ReqFolderCreateDto dto,
            @AuthenticationPrincipal CustomUser user
    ) {
        ResFolderDto result = folderService.createDeptFolder(user, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "부서 폴더 생성 성공", result));
    }

    @PostMapping("/prvt")
    public ResponseEntity<ResponseDto<ResFolderDto>> createPrvt(
            @RequestBody @Valid ReqFolderCreateDto dto,
            @AuthenticationPrincipal CustomUser user
    ) {
        ResFolderDto result = folderService.createPrvtFolder(user, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "개인 폴더 생성 성공", result));
    }

    @GetMapping("/dept")
    public ResponseEntity<ResponseDto<List<ResFolderDto>>> listDept(
            @RequestParam(required = false, name = "parentId") Long parentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "부서 폴더 조회 성공",
                folderService.listDept(user, parentId)));
    }

    @GetMapping("/prvt")
    public ResponseEntity<ResponseDto<List<ResFolderDto>>> listPrvt(
            @RequestParam(required = false, name = "parentId") Long parentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "개인 폴더 조회 성공",
                folderService.listPrvt(user, parentId)));
    }

    @PatchMapping("/{folderNo}/rename")
    public ResponseEntity<ResponseDto<ResFolderDto>> rename(
            @PathVariable(name = "folderNo") Long folderNo,
            @RequestBody @Valid ReqRenameDto dto,
            @AuthenticationPrincipal CustomUser user
    ) {
        ResFolderDto res = folderService.rename(user, folderNo, dto);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "폴더명 변경 성공", res));
    }

    @PostMapping("/attachments/{attachmentId}/move")
    public ResponseEntity<ResponseDto<Void>> moveAttachment(
            @PathVariable(name = "attachmentId") Long attachmentId,
            @RequestParam(name = "toFolderNo") Long toFolderNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        folderService.moveCloudAttachment(user, attachmentId, toFolderNo);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "파일 이동 성공", null));
    }

    @DeleteMapping("/{folderNo}")
    public ResponseEntity<ResponseDto<Void>> delete(@PathVariable(name = "folderNo") Long folderNo,
                                                    @AuthenticationPrincipal CustomUser user) {
        folderService.deleteFolderTree(user, folderNo);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "폴더 삭제 성공", null));
    }
}

