package com.multi.mlpenterpriseapprovalsystem.meeting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String selectMeeting(){
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
