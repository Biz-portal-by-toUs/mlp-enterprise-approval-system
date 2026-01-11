package com.multi.mlpenterpriseapprovalsystem.dashboard.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.dashboard.dto.ResDashboardDto;
import com.multi.mlpenterpriseapprovalsystem.dashboard.service.DashboardService;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto.ResScheduleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 메인페이지 대시보드 메일, 결재 관련 요약 컨트롤러
 *
 * @author : 권지영
 * @filename : DashboardController
 * @since : 2026. 1. 6. 화요일
 */
@RestController
@RequestMapping("/api/v1/dashboard/summary")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ResponseDto<ResDashboardDto>> getDashboardSummary(@AuthenticationPrincipal CustomUser user) {

        ResDashboardDto res = dashboardService.getSummary(user);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "메일, 결재 관련 요약 성공", res));
    }

    @GetMapping("/calendar")
    public ResponseEntity<ResponseDto<List<ResScheduleDto>>> getDashboardCalendar(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(name = "baseDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate,
            @RequestParam(name = "startDay", required = false, defaultValue = "1") int startDay // 1=월, 0=일
    ) {
        LocalDate targetDate = (baseDate != null) ? baseDate : LocalDate.now();

        List<ResScheduleDto> schedules = dashboardService.getCombinedSchedules(user, targetDate, startDay);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "대시보드 통합 일정 조회 성공", schedules));
    }
}
