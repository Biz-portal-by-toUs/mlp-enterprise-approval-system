package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/documents")
    public String viewFinalizedDocuments(@RequestParam(name = "status") String status) {
        if(status.equals("FINALIZED")){
            return "document/finalized-list";
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }

    @GetMapping("/documents/me")
    public String viewDocumentsByStatus(@RequestParam(name = "status") String status) {
        if(status.equals("SUBMITTED")) {
            return "/document/submitted-list";
        }
        else if(status.equals("AWAITING")){
            return "/document/awaiting-list";
        }
        else if(status.equals("PROCESSED")){
            return "/document/processed-list";
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }

}
