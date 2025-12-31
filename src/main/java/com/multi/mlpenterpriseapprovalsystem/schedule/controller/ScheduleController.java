package com.multi.mlpenterpriseapprovalsystem.schedule.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ReqScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ResScheduleListDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사원 개인 일정 관련 컨트롤러
 *
 * @author : 권지영
 * @filename : ScheduleController
 * @since : 2025. 12. 30. 화요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/calendar")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/items")
    public ResponseEntity<ResponseDto<ResScheduleListDto>> getPersonalSchedules(
            @AuthenticationPrincipal CustomUser user,
            @Valid @ModelAttribute ReqScheduleDto request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "일정 조회 성공", scheduleService.getItems(user, request)));
    }
}
