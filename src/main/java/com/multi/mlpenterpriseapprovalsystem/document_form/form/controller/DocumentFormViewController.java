package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormViewController
 * @since : 2025-12-23 화요일
 */

@Controller
@RequestMapping("/document=form/manager")
public class DocumentFormViewController {

    @GetMapping("/forms")
    public String formListPage() {
        // templates/document-form/manager/form/form-list.html
        return "document-form/manager/form/form-list";
    }
}
