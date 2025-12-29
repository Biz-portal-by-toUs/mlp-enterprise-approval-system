package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.ResDocumentFormDetailDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.ResDocumentFormListDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.service.DocumentFormService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.*;
import org.springframework.security.core.annotation.*;
import org.springframework.web.bind.annotation.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocFormController
 * @since : 2025-12-22 월요일
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
            @RequestBody ReqDocumentFormCreateDto req
    ) {
        Long docfoNo = documentFormService.createDocumentForm(
                req,
                customUser.getComId(),
                customUser.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(docfoNo);
    }

    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @PutMapping("/{docfoNo}")
    public ResponseEntity<Long> updateDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo,
            @RequestBody ReqDocumentFormCreateDto req
    ) {
        Long newDocfoNo = documentFormService.updateDocumentForm(
                docfoNo,
                req,
                customUser.getComId(),
                customUser.getUsername()
        );
        return ResponseEntity.ok(newDocfoNo);
    }

    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @DeleteMapping("/{docfoNo}")
    public ResponseEntity<Void> deleteDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo
    ) {
        documentFormService.deleteDocumentForm(docfoNo, customUser.getComId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 회사별 + 상태별 목록 조회 (+ 제목 검색)
     * - stat: A/P/R/T/D 등
     * - q 또는 docfoName: 제목 키워드 검색(Containing)
     */
    @GetMapping
    public ResponseEntity<Page<ResDocumentFormListDto>> getForms(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "stat", defaultValue = "A") DocumentFormStats stat,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "docfoName", required = false) String docfoName,
            @PageableDefault(size = 15) Pageable pageable
    ) {
        String keyword = (q != null && !q.isBlank()) ? q : docfoName;

        return ResponseEntity.ok(
                documentFormService.findListByStatus(stat, customUser.getComId(), keyword, pageable)
        );
    }

    @GetMapping("/{docfoNo}")
    public ResponseEntity<ResDocumentFormDetailDto> getDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo
    ) {
        ResDocumentFormDetailDto result =
                documentFormService.findDetailById(docfoNo, customUser.getComId());
        return ResponseEntity.ok(result);
    }

    /**
     * 승인/반려 상태 변경
     * - PATCH /api/v1/forms/{docfoNo}/status
     * - body: { "docfoStat":"A" }
     * - body: { "docfoStat":"R", "rejectReason":"..." }
     */
    @PreAuthorize("hasAnyRole('COM_ADMIN','SEC_ADMIN')")
    @PatchMapping("/{docfoNo}/status")
    public ResponseEntity<Void> changeStatus(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo,
            @RequestBody ReqDocumentFormStatusDto req
    ) {
        if (req == null || req.docfoStat() == null) {
            return ResponseEntity.badRequest().build();
        }

        documentFormService.changeStatus(
                docfoNo,
                customUser.getComId(),
                req.docfoStat(),
                req.rejectReason()
        );

        return ResponseEntity.noContent().build();
    }
}