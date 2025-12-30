package com.multi.mlpenterpriseapprovalsystem.prov_document.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 사내 규정 뷰 컨트롤러
 *
 * @author : 김승기
 * @filename : ViewProvDocumentController
 * @since : 2025. 12. 29. 월요일
 */
@Controller
@RequestMapping("/prov-documents")
public class ViewProvDocumentController {
    @GetMapping("/create")
    public String create() {
        return "/prov-document/prov-create";
    }
}
