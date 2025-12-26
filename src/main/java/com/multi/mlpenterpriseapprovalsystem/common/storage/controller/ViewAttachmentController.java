package com.multi.mlpenterpriseapprovalsystem.common.storage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * attachment 뷰 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewAttachmentController
 * @since : 2025. 12. 24. 수요일
 */
@Controller
public class ViewAttachmentController {

    @GetMapping("/attachment-test")
    public String attachmentTestPage() {
        // templates/attachment-test.html
        return "attachment-test";
    }
}
