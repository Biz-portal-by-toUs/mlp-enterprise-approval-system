package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 문서 화면용 컨트롤러
 *
 * @author : 이지헌
 * @filename : ViewDocumentController
 * @since : 25. 12. 19. 금요일
 */

@Controller
@Slf4j
@RequiredArgsConstructor
public class ViewDocumentController {

    private final DocumentService documentService;

    // 최종승인된 문서 조회 화면
    @GetMapping("/documents")
    public String viewFinalizedDocuments(@RequestParam(name = "status") String status) {
        if(status.equals("FINALIZED")){
            return "document/finalized-list";
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }

    // 임시저장한 문서, 상신한 문서, 결재할 문서, 결재한 조회 문서 화면
    @GetMapping("/documents/me")
    public String viewDocumentsByStatus(@RequestParam(name = "status") String status) {
        if("UNSUBMITTED".equals(status)) {
            return "document/unsubmitted-list";
        }
        else if(status.equals("SUBMITTED")) {
            return "document/submitted-list";
        }
        else if(status.equals("AWAITING")){
            return "document/awaiting-list";
        }
        else if(status.equals("PROCESSED")){
            return "document/processed-list";
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }


    // 반려된 문서 재작성 화면
    @GetMapping("/documents/{docNo}/rejected/rewrite")
    public String documentRewriteRejected(@PathVariable(name = "docNo") Long docNo,
                                          @RequestParam(name = "atteNo", required = false) Long atteNo) {
        return "document/document-rejected-rewrite";
    }

    // 문서양식 리스트 화면
    @GetMapping("/document-forms")
    public String viewDocumentForms() {
        return "document/documentform-list";
    }


    // 문서 작성 화면
    @GetMapping("/documents/create")
    public String viewNewDocument(@RequestParam(name = "docfoNo") Long docfoNo,
                                  @RequestParam(name = "atteNo", required = false) Long atteNo,
                                  @RequestParam(name = "category", required = false) String category)
    {
        return "document/create";
    }

    // 문서 상세 조회 화면
//    @GetMapping("/documents/{docNo}")
//    public String viewDocumentDetailByDocNo(@RequestParam(name = "status", defaultValue = "FINALIZED") String status,
//                                            @RequestParam(name = "atteNo", required = false) Long atteNo)
//    {
//        if("UNSUBMITTED".equals(status)) { // 임시저장 문서 상세
//            return "document/document-temp-rewrite";
//        }
//        else if("SUBMITTED".equals(status)){ // 상신한 문서 상세
//            return "document/submitted-detail";
//        }
//        else if("AWAITING".equals(status)){ // 결재할 문서 상세
//            return "document/awaiting-detail";
//        }
//        else if("PROCESSED".equals(status)){ // 결재한 문서 상세
//            return "document/processed-detail";
//        }
//        else if("FINALIZED".equals(status)){ // 최종승인 문서 상세
//            return "document/finalized-detail";
//        }
//        else{
//            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
//        }
//    }

    @GetMapping("/documents/{docNo}")
    public String viewDocumentDetailByDocNo(
            @PathVariable(name = "docNo") Long docNo,
            @RequestParam(name = "status", defaultValue = "FINALIZED") String status,
            @RequestParam(name = "atteNo", required = false) Long atteNo,
            @AuthenticationPrincipal CustomUser customUser) {

        // 1. 서비스의 단일 쿼리 로직을 통해 "현재 이 사용자에게 가장 적합한 상태"를 가져옴
        String actualStatus = documentService.determineActualStatus(
                customUser.getComId(),
                customUser.getUsername(),
                docNo,
                status
        );

        // 2. 요청한 상태와 실제 판별된 상태가 다르면 리다이렉트 (주소창 변경)
        if (!status.equals(actualStatus)) {
            log.info("Status mismatch [docNo: {}]: requested={}, actual={}. Redirecting...", docNo, status, actualStatus);

            String redirectUrl = "redirect:/documents/" + docNo + "?status=" + actualStatus;
            if (atteNo != null) {
                redirectUrl += "&atteNo=" + atteNo;
            }
            return redirectUrl;
        }

        // 3. 상태에 맞는 템플릿 반환 (actualStatus와 status가 일치하는 경우)
        return switch (status) {
            case "UNSUBMITTED" -> "document/document-temp-rewrite"; // 임시저장 수정
            case "SUBMITTED"   -> "document/submitted-detail";      // 상신 문서 상세 (반려 시 재작성 가능)
            case "AWAITING"    -> "document/awaiting-detail";       // 결재 대기 상세
            case "PROCESSED"   -> "document/processed-detail";      // 내가 결재한 문서 상세
            case "FINALIZED"   -> "document/finalized-detail";      // 최종 승인 상세
            default -> throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        };
    }

    // 문서 인쇄 전용 페이지
    @GetMapping("/documents/{docNo}/print")
    public String viewDocumentPrint(@PathVariable(name = "docNo") Long docNo,
                                    @RequestParam(name = "status", defaultValue = "SUBMITTED") String status) {
        // 모든 상태의 문서에 대해 동일한 인쇄 페이지 반환
        return "document/print";
    }
}
