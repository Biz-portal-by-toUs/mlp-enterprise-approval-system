package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

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
import org.springframework.web.bind.annotation.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocFormController
 * @since : 2025-12-22 월요일
 */

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
public class DocumentFormController {

    private final DocumentFormService documentFormService;

    @PostMapping
    public ResponseEntity<Long> createDocumentForm(
            @RequestBody ReqDocumentFormCreateDto req
    ) {
        Long docfoNo = documentFormService.createDocumentForm(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(docfoNo);
    }

    @PutMapping("/{docfoNo}")
    public ResponseEntity<Long> updateDocumentForm(
            @PathVariable(name="docfoNo") Long docfoNo,
            @RequestBody ReqDocumentFormCreateDto req
    ) {
        Long newDocfoNo = documentFormService.updateDocumentForm(docfoNo, req);
        return ResponseEntity.ok(newDocfoNo);
    }

    @DeleteMapping("/{docfoNo}")
    public ResponseEntity<Void> deleteDocumentForm(
            @PathVariable(name="docfoNO") Long docfoNo
    ) {
        documentFormService.deleteDocumentForm(docfoNo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ResDocumentFormListDto>> getApprovedForms(
            @PageableDefault(size = 15) Pageable pageable
    ) {
        return ResponseEntity.ok(
                documentFormService.findListByStatus(DocumentFormStats.A, pageable)
        );
    }

    @GetMapping("/{docfoNo}")
    public ResponseEntity<ResDocumentFormDetailDto> getDocumentForm(
            @PathVariable(name="docfoNo") Long docfoNo
    ) {
        ResDocumentFormDetailDto result =
                documentFormService.findDetailById(docfoNo);

        return ResponseEntity.ok(result);
    }
}