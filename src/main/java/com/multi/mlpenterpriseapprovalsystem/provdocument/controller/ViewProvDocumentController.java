package com.multi.mlpenterpriseapprovalsystem.provdocument.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 사내 규정 뷰 컨트롤러
 *
 * @author : 김승기
 * @filename : ViewProvDocumentController
 * @since : 2025. 12. 29. 월요일
 */
@Controller
@RequestMapping("/admin/prov-document")
public class ViewProvDocumentController {
    // 등록 페이지
    @GetMapping("/create")
    public String create(Model model) {
        return "prov-document/prov-document-create";
    }

    // 목록 페이지
    @GetMapping("/list")
    public String list(Model model) {
        return "prov-document/prov-document-list";
    }

    // 상세 페이지
    @GetMapping("/{provNo}")
    public String detail(@PathVariable(name = "provNo") Long provNo, Model model) {
        model.addAttribute("provNo", provNo); // ✅ JS에서 /*[[${provNo}]]*/ 로 읽을 값
        return "prov-document/prov-document-detail";
    }

    // 수정 페이지
    @GetMapping("/{provNo}/update")
    public String edit(@PathVariable(name = "provNo") Long provNo, Model model) {
        model.addAttribute("provNo", provNo); // ✅ 수정 대상 번호를 모델에 전달
        return "prov-document/prov-document-update";
    }
}
