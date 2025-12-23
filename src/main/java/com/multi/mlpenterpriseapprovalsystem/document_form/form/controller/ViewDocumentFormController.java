package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 문서양식 화면(View) 라우팅 컨트롤러
 *
 * - 목록: /document-form/manager/form/forms
 * - 생성: /document-form/manager/form/make-form
 * - 수정: /document-form/manager/form/update-form?docfoNo=...
 * - 상세: /document-form/manager/form/{docfoNo}
 */
@Controller
@RequestMapping("/document-form/manager/form")
public class ViewDocumentFormController {

    // 목록 화면
    @GetMapping("/forms")
    public String formListPage() {
        return "document-form/manager/form/form-list";
    }

    // 생성 화면
    @GetMapping("/make-form")
    public String makeForm() {
        return "document-form/manager/form/make-form";
    }

    // 수정 화면
    @GetMapping("/update-form")
    public String updateForm(@RequestParam Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/manager/form/update-form";
    }

    // 상세 화면
    @GetMapping("/{docfoNo}")
    public String detail(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/manager/form/detail";
    }
}