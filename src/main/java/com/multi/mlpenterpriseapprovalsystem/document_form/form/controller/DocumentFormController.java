package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req.ReqDocumentFormCreateDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req.ReqDocumentFormStatusDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.ResDocumentFormDetailDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.ResDocumentFormListDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.service.DocumentFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 문서양식 REST API 컨트롤러
 *
 * - 생성/수정/삭제요청/상태변경/삭제승인/삭제반려
 * - 목록/상세조회
 */
@RestController
@RequestMapping("/api/v1/forms")
@RequiredArgsConstructor
public class DocumentFormController {

    private final DocumentFormService documentFormService;

    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PostMapping
    public ResponseEntity<Long> createDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @Valid @RequestBody ReqDocumentFormCreateDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        Long docfoNo = documentFormService.createDocumentForm(
                req, customUser.getComId(), customUser.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(docfoNo);
    }

    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PutMapping("/{docfoNo}")
    public ResponseEntity<Long> updateDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo,
            @Valid @RequestBody ReqDocumentFormCreateDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        Long newDocfoNo = documentFormService.updateDocumentForm(
                docfoNo, req, customUser.getComId(), customUser.getUsername()
        );
        return ResponseEntity.ok(newDocfoNo);
    }

    /**
     * 삭제 "요청" (상태를 W로 전환하는 비즈니스라면 Service에서 처리)
     */
    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @DeleteMapping("/{docfoNo}")
    public ResponseEntity<Void> requestDelete(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        documentFormService.requestDelete(docfoNo, customUser.getComId(), customUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * 목록 조회
     * - /api/v1/forms?stat=A&stat=X&q=...
     * - stat 파라미터가 없으면 defaultValue="A"
     */
    @GetMapping
    public ResponseEntity<Page<ResDocumentFormListDto>> getForms(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "stat", defaultValue = "A") List<DocumentFormStats> stat,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "docfoName", required = false) String docfoName,
            @PageableDefault(size = 15) Pageable pageable
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);

        String keyword = (q != null && !q.isBlank()) ? q : docfoName;

        // 혹시라도 빈 리스트로 들어오는 케이스 방어 (프론트 버그/요청 오류)
        if (CollectionUtils.isEmpty(stat)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ResponseEntity.ok(
                documentFormService.findListByStatuses(stat, customUser.getComId(), keyword, pageable)
        );
    }

    @GetMapping("/{docfoNo}")
    public ResponseEntity<ResDocumentFormDetailDto> getDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        return ResponseEntity.ok(
                documentFormService.findDetailById(docfoNo, customUser.getComId())
        );
    }

    /**
     * 상태 변경 (승인/반려 등)
     * - req.docfoStat 필수 → @Valid로 처리
     */
    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN')")
    @PatchMapping("/{docfoNo}/status")
    public ResponseEntity<Void> changeStatus(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo,
            @Valid @RequestBody ReqDocumentFormStatusDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        // 반려/삭제반려 사유가 필요한 경우는 서비스에서 도메인 규칙으로 검사하고
        // 필요 시 ErrorCode.REJECT_REASON_REQUIRED 또는 별도 코드로 던지는 걸 권장
        documentFormService.changeApproveOrReject(
                docfoNo,
                customUser.getComId(),
                req.docfoStat(),
                req.rejectReason()
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * 삭제 승인
     */
    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN')")
    @PatchMapping("/{docfoNo}/delete-approve")
    public ResponseEntity<Void> approveDelete(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        documentFormService.approveDelete(docfoNo, customUser.getComId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 삭제 반려
     * - 사유 필수: 프론트에서 팝업으로 받을 거면 ErrorCode.REJECT_REASON_REQUIRED 쓰면 깔끔
     */
    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN')")
    @PatchMapping("/{docfoNo}/delete-reject")
    public ResponseEntity<Void> rejectDelete(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo,
            @Valid @RequestBody ReqDocumentFormStatusDto req
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (docfoNo == null || docfoNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        if (req.rejectReason() == null || req.rejectReason().isBlank()) {
            throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        documentFormService.rejectDelete(docfoNo, customUser.getComId(), req.rejectReason());
        return ResponseEntity.noContent().build();
    }
}
