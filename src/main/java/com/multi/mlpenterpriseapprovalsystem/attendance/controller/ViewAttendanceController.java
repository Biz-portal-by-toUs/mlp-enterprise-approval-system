package com.multi.mlpenterpriseapprovalsystem.attendance.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 근태 화면용 컨트롤러
 *
 * @author : 이지헌
 * @filename : ViewAttendanceController
 * @since : 25. 12. 31. 수요일
 */
@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/attendances")
public class ViewAttendanceController {

    // 내 휴가 관리 화면
    @GetMapping("/me/vacations")
    public String viewVacations() {
        return "attendance/vacation/list";
    }

    // 내 출장 관리 화면
    @GetMapping("/me/business-trips")
    public String viewBusinessTrips() {
        return "attendance/business-trip/list";
    }

    // 전체 근태 조회 화면
    @GetMapping("")
    public String viewAttendances() {
        return "attendance/list";
    }

}
