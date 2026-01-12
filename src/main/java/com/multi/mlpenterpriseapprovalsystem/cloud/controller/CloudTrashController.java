package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResAdminTrashLogDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ReqTrashPurgeSelected;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResAdminTrashLogSliceDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ResTrashItemDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import com.multi.mlpenterpriseapprovalsystem.cloud.service.CloudTrashPurgeService;
import com.multi.mlpenterpriseapprovalsystem.cloud.service.CloudTrashService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 클라우드 휴지통(삭제 항목) 관련 API를 제공하는 컨트롤러입니다.
 *
 * 휴지통 목록 조회(개인함/부서함)<br>
 * 배치 단위 복구(삭제 작업 batchId 기준 복구)<br>
 * 개인함 휴지통 비우기(완전 삭제)<br>
 * 관리자 삭제/복구/완전삭제 감사 로그 조회<br>
 * 개인함 선택 항목 완전 삭제
 *
 * @author : 송현님
 * @filename : CloudTrashController
 * @since : 2026-01-05 오전 12:29 월요일
 */

@RestController
@RequestMapping("/api/v1/cloud/trash")
@RequiredArgsConstructor
public class CloudTrashController {

    private final CloudTrashService cloudTrashService;
    private final CloudTrashPurgeService cloudTrashPurgeService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<ResTrashItemDto>>> list(
            @RequestParam(name = "scope") FolderScope scope, // DEPT or PRVT
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal CustomUser user
    ) {
        return ResponseEntity.ok(new ResponseDto<>(
                HttpStatus.OK, "휴지통 조회 성공",
                cloudTrashService.listTrash(user, scope, limit, offset)
        ));
    }

    @PostMapping("/restore/{batchId}")
    public ResponseEntity<ResponseDto<Void>> restore(
            @PathVariable(name = "batchId") String batchId,
            @RequestParam(name = "scope") FolderScope scope,
            @AuthenticationPrincipal CustomUser user
    ) {
        cloudTrashService.restoreByBatch(user, batchId, scope);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "복구 성공", null));
    }

    @PostMapping("/empty/prvt")
    public ResponseEntity<ResponseDto<Void>> emptyMyTrash(
            @AuthenticationPrincipal CustomUser user
    ) {
        cloudTrashService.purgeMyPrvtTrash(user);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "개인함 휴지통 비우기 성공", null));
    }

    @GetMapping("/admin-log")
    public ResponseEntity<ResponseDto<ResAdminTrashLogSliceDto>> adminLog(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam String scope,
            @RequestParam(defaultValue="10") int limit,
            @RequestParam(defaultValue="0") int offset,
            @RequestParam(required=false, defaultValue="") String q,
            @RequestParam String from,
            @RequestParam String to
    ){
        FolderScope s = FolderScope.valueOf(scope.toUpperCase());

        ResAdminTrashLogSliceDto result =
                cloudTrashService.listAdminActionLogs(user, s, limit, offset, from, to, q);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "조회 성공", result));
    }

    @PostMapping("/purge/prvt")
    public ResponseEntity<ResponseDto<Void>> purgeSelectedPrvt(
            @RequestBody ReqTrashPurgeSelected req,
            @AuthenticationPrincipal CustomUser user
    ) {
        cloudTrashService.purgeSelectedPrvt(user, req.getItems());
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "개인함 선택 삭제 성공", null));
    }

    @GetMapping("/admin-log/children")
    public ResponseEntity<ResponseDto<List<ResAdminTrashLogDto>>> adminLogChildren(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(name = "batchId") String batchId,
            @RequestParam(name = "folderNo") Long folderNo
    ){
        List<ResAdminTrashLogDto> result =
                cloudTrashService.listAdminLogChildren(user, batchId, folderNo);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "조회 성공", result));
    }

    @PostMapping("/purge/dept/file/{attachmentId}")
    public ResponseEntity<ResponseDto<Void>> purgeDeptFile(
            @PathVariable(name = "attachmentId") Long attachmentId,
            @AuthenticationPrincipal CustomUser user
    ){
        cloudTrashService.purgeDeptFile(user, attachmentId);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "부서 파일 영구삭제 성공", null));
    }

    @PostMapping("/purge/dept/folder/{folderNo}")
    public ResponseEntity<ResponseDto<Void>> purgeDeptFolder(
            @PathVariable(name = "folderNo") Long folderNo,
            @AuthenticationPrincipal CustomUser user
    ){
        cloudTrashService.purgeDeptFolder(user, folderNo);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "부서 폴더 영구삭제 성공", null));
    }
}
