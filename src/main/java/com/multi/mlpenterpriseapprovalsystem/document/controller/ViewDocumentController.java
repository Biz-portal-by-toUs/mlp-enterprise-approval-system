package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 문서 관련 화면용 컨트롤러
 *
 * @author : 이지헌
 * @filename : ViewDocumentController
 * @since : 25. 12. 19. 금요일
 */

@Controller
@Slf4j
@RequiredArgsConstructor
public class ViewDocumentController {

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
    public String documentRewriteRejected(@PathVariable(name = "docNo") Long docNo) {
        return "document/document-rejected-rewrite";
    }

    // 문서양식 리스트 화면
    @GetMapping("/document-forms")
    public String viewDocumentForms() {
        return "document/documentform-list";
    }


    // 문서 작성 화면
    @GetMapping("/documents/create")
    public String viewNewDocument(@RequestParam(name = "docfoNo") Long docfoNo) {
        return "document/create";
    }

    // 문서 상세 조회 화면
    @GetMapping("/documents/{docNo}")
    public String viewDocumentDetailByDocNo(@RequestParam(name = "status", defaultValue = "FINALIZED") String status)
    {
        if("UNSUBMITTED".equals(status)) { // 임시저장 문서 상세
            return "document/document-temp-rewrite";
        }
        else if("SUBMITTED".equals(status)){ // 상신한 문서 상세
            return "document/submitted-detail";
        }
        else if("AWAITING".equals(status)){ // 결재할 문서 상세
            return "document/awaiting-detail";
        }
        else if("PROCESSED".equals(status)){ // 결재한 문서 상세
            return "document/processed-detail";
        }
        else if("FINALIZED".equals(status)){ // 최종승인 문서 상세
            return "document/finalized-detail";
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }
}
