package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문서 처리 컨트롤러
 *
 * @author : 이지헌
 * @filename : DocumentController
 * @since : 25. 12. 15. 월요일
 */

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    // 해당 회사의 문서의 결재라인 고려하여 반환
    @GetMapping("/documents")
    public ResponseEntity<ResponseDto<Page<ResDocumentDto>>> getApprovedDocuments(@RequestParam(name = "com_id") String comId,
                                                                                  @RequestParam(name = "status") String status,
                                                                                  @RequestParam(name = "page", defaultValue = "0") int page) {

        Pageable pageable = PageRequest.of(page, 10);

        Page<ResDocumentDto> resDocumentDtos = documentService.getDocumentsByStatus(comId, status, pageable);

        String message = resDocumentDtos.isEmpty() ? "최종 승인된 문서가 없습니다" : "최종 승인된 모든 문서 조회 성공";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, resDocumentDtos));
    }



}
