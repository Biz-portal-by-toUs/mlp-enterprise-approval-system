package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.service.DocumentServiceV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/api/v2")
public class DocumentControllerV2 {

    private final DocumentServiceV2 documentServiceV2;

    // 회사 전체의 문서를 문서상태 기준 조회
    @GetMapping("/documents")
    public ResponseEntity<ResponseDto<Page<ResDocumentDto>>> getDocumentsByStatus(@AuthenticationPrincipal CustomUser customUser,
                                                                                  @ModelAttribute ReqDocumentDto reqDocumentDto,
                                                                                  @RequestParam(name = "status") String status,
                                                                                  @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                  @RequestParam(name = "sort", defaultValue = "") String sort) {

        Page<ResDocumentDto> resDocumentDtos = documentServiceV2.getDocumentsByStatus(customUser.getComId(), customUser.getUsername(), reqDocumentDto, status, page, sort);

        String message = "";
        if("FINALIZED".equals(status)){
            message = resDocumentDtos.isEmpty() ? "최종 승인된 문서가 없습니다" : "최종 승인된 문서 조회 성공";
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, resDocumentDtos));
    }

    // 내가 상신한 문서 조회(status = "SUBMITTED"), 내가 결재할 문서 조회(status = "AWAITING"), 내가 결재한 문서 조회(status = "PROCESSED")
    @GetMapping("/documents/me")
    public ResponseEntity<ResponseDto<Page<ResDocumentDto>>> getMyDocumentsByStatus(@AuthenticationPrincipal CustomUser customUser,
                                                                                     @ModelAttribute ReqDocumentDto reqDocumentDto,
                                                                                     @RequestParam(name = "status") String status,
                                                                                     @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                     @RequestParam(name = "sort", defaultValue = "") String sort){

        Page<ResDocumentDto> resDocumentDtos = documentServiceV2.getMyDocumentsByStatus(customUser.getComId(), customUser.getUsername(), reqDocumentDto, status, page, sort);

        String message = "";
        if("SUBMITTED".equals(status)) {
            message = resDocumentDtos.isEmpty() ? "내가 상신한 문서가 없습니다" : "내가 상신한 문서 조회 성공";
        }
        else if("AWAITING".equals(status)) {
            message = resDocumentDtos.isEmpty() ? "내가 결재할 문서가 없습니다" : "내가 결재할 문서 조회 성공";
        }
        else if("PROCESSED".equals(status)) {
            message = resDocumentDtos.isEmpty() ? "내가 결재한 문서가 없습니다" : "내가 결재한 문서 조회 성공";
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, message, resDocumentDtos));
    }




}
