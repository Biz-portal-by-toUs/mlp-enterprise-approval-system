package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import lombok.*;
import org.springframework.security.access.prepost.*;
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

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN','EMPLOYEE')")
    @GetMapping("/forms")
    public String formList() {
        return "document-form/form-list";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN','EMPLOYEE')")
    @GetMapping("/{docfoNo}")
    public String formDetail(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/detail";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/new")
    public String createForm() {
        return "document-form/make-form";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/{docfoNo}/edit")
    public String updateForm(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/update-form";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/pending")
    public String pendingList() {
        return "document-form/pending-form-list";
    }

    @GetMapping("/reject-reason")
    public String rejectReasonPopup(
            @RequestParam(required = false) Long docfoNo,
            @RequestParam(defaultValue = "view") String mode,
            Model model
    ) {
        model.addAttribute("docfoNo", docfoNo);
        model.addAttribute("mode", mode); // view | reject | delReject
        return "document-form/form-reject-reason";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/temp")
    public String tempList() {
        return "document-form/temp-form-list";
    }
}