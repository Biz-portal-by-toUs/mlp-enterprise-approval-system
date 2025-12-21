package com.multi.mlpenterpriseapprovalsystem.notice.controller;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : FileViewController
 * @since : 2025-12-21 일요일
 */
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notice")
public class FileViewController {

    @GetMapping("/files")
    public String filesPage() {
        return "notice/files"; // templates/files.html
    }
}
