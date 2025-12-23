package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormViewController
 * @since : 2025-12-23 화요일
 */

@Controller
@RequestMapping("/document-form/manager")
public class DocumentFormViewController {

    @GetMapping("/forms")
    public String formListPage() {
        return "document-form/manager/form/form-list";
    }

    // 생성/편집 화면
    @GetMapping("/make-form")
    public String makeForm() {
        return "document-form/manager/form/make-form";
    }

    // 상세 화면
    @GetMapping("/{docfoNo}")
    public String detail(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/manager/form/detail";
    }
}
