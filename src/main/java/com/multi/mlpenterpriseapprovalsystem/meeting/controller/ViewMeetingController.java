package com.multi.mlpenterpriseapprovalsystem.meeting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
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
        return "meeting/create";
    }
}
