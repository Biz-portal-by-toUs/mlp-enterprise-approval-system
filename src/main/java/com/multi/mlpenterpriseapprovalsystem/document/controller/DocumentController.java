package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<ResponseDto<Page<ResDocumentDto>>> getApprovedDocuments(@AuthenticationPrincipal CustomUser customUser,
                                                                                  @RequestParam(name = "status") String status,
                                                                                  @RequestParam(name = "page", defaultValue = "0") int page) {

        Page<ResDocumentDto> resDocumentDtos = documentService.getDocumentsByStatus(customUser.getComId(), customUser.getUsername(), status, page);

        String message = resDocumentDtos.isEmpty() ? "최종 승인된 문서가 없습니다" : "최종 승인된 문서 조회 성공";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, resDocumentDtos));
    }

    // 내가 상신한 문서 조회(status = "ANY"), 내가 결재할 문서 조회(status = "AWAITING"), 내가 결재한 문서 조회(status = "APPROVED")
    @GetMapping("/documents/me")
    public ResponseEntity<ResponseDto<Page<ResDocumentDto>>> getMySubmittedDocuments(@AuthenticationPrincipal CustomUser customUser,
                                                                                     @RequestParam(name = "status") String status,
                                                                                     @RequestParam(name = "page", defaultValue = "0") int page){

        Page<ResDocumentDto> resDocumentDtos = documentService.getDocumentsByStatus(customUser.getComId(), customUser.getUsername(), status, page);

        String message = "";
        if(status.equals("ANY")) {
            message = resDocumentDtos.isEmpty() ? "내가 상신한 문서가 없습니다" : "내가 상신한 문서 조회 성공";
        }
        else if(status.equals("AWAITING")) {
            message = resDocumentDtos.isEmpty() ? "내가 결재할 문서가 없습니다" : "내가 결재할 문서 조회 성공";
        }
        else if(status.equals("APPROVED")) {
            message = resDocumentDtos.isEmpty() ? "내가 결재한 문서가 없습니다" : "내가 결재한 문서 조회 성공";
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, resDocumentDtos));
    }



}
