package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.TrashItemDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import com.multi.mlpenterpriseapprovalsystem.cloud.service.CloudTrashService;
import com.multi.mlpenterpriseapprovalsystem.cloud.service.FolderService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Please explain the class!!!
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

    @GetMapping
    public ResponseEntity<ResponseDto<List<TrashItemDto>>> list(
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

    // 개인함 휴지통 비우기
    @PostMapping("/empty/prvt")
    public ResponseEntity<ResponseDto<Void>> emptyMyTrash(
            @AuthenticationPrincipal CustomUser user
    ) {
        // 서비스에 purgeMyPrvtTrash(...) 만들어서 호출
        // 공유함은 엔드포인트 자체를 안 만들면 정책이 아주 명확해짐
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "개인함 휴지통 비우기 성공", null));
    }
}
