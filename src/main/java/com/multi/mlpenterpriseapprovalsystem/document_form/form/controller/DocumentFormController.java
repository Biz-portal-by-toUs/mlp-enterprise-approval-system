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

    @PostMapping
    public ResponseEntity<Long> createDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestBody ReqDocumentFormCreateDto req
    ) {
        Long docfoNo = documentFormService.createDocumentForm(req, customUser.getComId(), customUser.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(docfoNo);
    }

    @PutMapping("/{docfoNo}")
    public ResponseEntity<Long> updateDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "docfoNo") Long docfoNo,
            @RequestBody ReqDocumentFormCreateDto req
    ) {
        Long newDocfoNo = documentFormService.updateDocumentForm(docfoNo, req, customUser.getComId(), customUser.getUsername());
        return ResponseEntity.ok(newDocfoNo);
    }

    @DeleteMapping("/{docfoNo}")
    public ResponseEntity<Void> deleteDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo
    ) {
        documentFormService.deleteDocumentForm(docfoNo, customUser.getComId());
        return ResponseEntity.noContent().build();
    }

    // 회사별 + 상태별 리스트 조회 (로그인 기반)
    @GetMapping
    public ResponseEntity<Page<ResDocumentFormListDto>> getForms(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "stat", defaultValue = "A") DocumentFormStats stat,
            @PageableDefault(size = 15) Pageable pageable
    ) {
        return ResponseEntity.ok(
                documentFormService.findListByStatus(stat, customUser.getComId(), pageable)
        );
    }

    @GetMapping("/{docfoNo}")
    public ResponseEntity<ResDocumentFormDetailDto> getDocumentForm(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long docfoNo
    ) {
        ResDocumentFormDetailDto result = documentFormService.findDetailById(docfoNo, customUser.getComId());
        return ResponseEntity.ok(result);
    }
}