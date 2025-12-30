package com.multi.mlpenterpriseapprovalsystem.meeting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Please explain the class!!!
 * 
 * @filename    : ViewMeetingController
 * @author      : 김승기
 * @since       : 2025. 12. 26. 금요일
 */
@Controller
@RequestMapping("/meeting")
public class ViewMeetingController {

    @GetMapping("/create")
    public String createMeeting(){
        return "meeting/meeting-create";
    }

    @GetMapping
    public String meetingList(@RequestParam(name="scope", defaultValue="ALL") String scope, Model model) {
        // scope 값에 따라 사이드바에 전달할 active 키워드 결정
        String activeMenu = "all"; // 기본값
        if ("MY_DEPT".equals(scope)) activeMenu = "dept";
        else if ("MY".equals(scope)) activeMenu = "my";

        model.addAttribute("active", activeMenu); // ✅ HTML의 ${active}로 전달됨
        return "meeting/meeting-list";
    }

    @GetMapping("/{meetNo}")
    public String meetingDetail(@PathVariable(name = "meetNo") Long meetNo, Model model) {
        model.addAttribute("meetNo", meetNo);
        return "meeting/meeting-detail";
    }

    @GetMapping("/{meetNo}/update")
    public String updateMeeting(@PathVariable(name = "meetNo") Long meetNo, Model model) {
        model.addAttribute("meetNo", meetNo);
        return "meeting/meeting-update";
    }

}
