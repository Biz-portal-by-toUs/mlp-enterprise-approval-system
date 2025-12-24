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
public class ViewNoticeController {

    @GetMapping("/Attachment-notice/{noticeNo}")
    public String noticeAttach(@PathVariable("noticeNo") Long noticeNo, Model model) {

        model.addAttribute("noticeNo", noticeNo);

        return "notice/Attachment-notice";
    }

    @GetMapping("/list")
    public String noticeList() {

        return "notice/notice-list";
    }

    // 상세 조회
    @GetMapping("/{noticeNo}")
    public String placeDetail(@PathVariable("noticeNo") Long noticeNo, Model model) {
        model.addAttribute("noticeNo", noticeNo);
        return "notice/notice-detail";
    }

    @GetMapping("/files")
    public String filesPage() {
        return "notice/files"; // templates/files.html
    }

    @GetMapping("/popup")
    public String popup() {

        return "notice/popup";
    }

    // 📍 리뷰 등록 폼 페이지
    @GetMapping("/form")
    public String noticeFormPage(Model model) {
        return "notice/notice-form";
    }

    //공지사항 업데이트
    @GetMapping("/update/{noticeNo}")
    public String updateNotice(@PathVariable("noticeNo") Long noticeNo, Model model) {
        model.addAttribute("noticeNo", noticeNo);
        System.out.println("Received noticeNo: " + noticeNo);


        return "notice/notice-update";
    }

    /** 공지 수정 페이지 이동 (단순 렌더링) */
    @GetMapping("/edit/{noticeNo}")
    public String noticeEditPage(@PathVariable("noticdNo") Long noticeNo) {
        return "notice/notice-update";
    }
}
