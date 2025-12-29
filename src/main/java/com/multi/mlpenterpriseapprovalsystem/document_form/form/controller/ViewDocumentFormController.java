package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import lombok.*;
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
@RequestMapping("/form")
@RequiredArgsConstructor
public class ViewDocumentFormController {

    @GetMapping("/forms")
    public String formList() {
        return "document-form/form-list";
    }

    @GetMapping("/{docfoNo}")
    public String formDetail(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/detail";
    }

    @GetMapping("/new")
    public String createForm() {
        return "document-form/make-form";
    }

    @GetMapping("/{docfoNo}/edit")
    public String updateForm(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/update-form";
    }

    @GetMapping("/pending")
    public String pendingList() {
        return "document-form/pending-form-list";
    }
}