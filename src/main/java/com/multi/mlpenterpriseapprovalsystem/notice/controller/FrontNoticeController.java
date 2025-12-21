package com.multi.mlpenterpriseapprovalsystem.notice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : FrontNoticeController
 * @since : 2025-12-17 수요일
 */
@Controller
@RequestMapping("/notice")
public class FrontNoticeController {

    @GetMapping("/list")
    public String noticeList() {

        return "notice/list";
    }

    // 상세 조회
    @GetMapping("/{noticeNo}")
    public String placeDetail(@PathVariable("noticeNo") int noticeNo, Model model) {
        model.addAttribute("noticeNo", noticeNo);
        return "notice/detail";

    }

    @GetMapping("/files")
    public String filesPage() {
        return "notice/files"; // templates/files.html
    }



}
