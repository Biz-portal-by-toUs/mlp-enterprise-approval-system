package com.multi.mlpenterpriseapprovalsystem.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 일정 관련 뷰 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewScheduleController
 * @since : 2025. 12. 30. 화요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/schedule")
public class ViewScheduleController {

    @GetMapping("/calendar")
    public String calendarPage() {

        return "schedule/calendar";
    }
}
