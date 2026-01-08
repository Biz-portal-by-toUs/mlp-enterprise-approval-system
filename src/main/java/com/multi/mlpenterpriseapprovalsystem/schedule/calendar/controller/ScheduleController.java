package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto.*;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<ResponseDto<ResScheduleListDto>> getPersonalSchedules(
            @AuthenticationPrincipal CustomUser user,
            @Valid @ModelAttribute ReqScheduleDto request
    ) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "일정 조회 성공", scheduleService.getItems(user, request)));
    }

    @PostMapping
    public ResponseEntity<ResponseDto<ResScheduleDto>> createSchedule(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody ReqCreateScheduleDto request
    ) {
        ResScheduleDto created = scheduleService.createItem(user, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "일정 등록 성공", created));
    }

    @DeleteMapping("/{schNo}")
    public ResponseEntity<ResponseDto<Void>> deleteSchedule(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="schNo") Long schNo,
            @Valid @ModelAttribute ReqDeleteScheduleDto request
    ) {
        scheduleService.deleteItem(user, schNo, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "일정 삭제 성공", null));
    }

    @PatchMapping("/{schNo}")
    public ResponseEntity<ResponseDto<Void>> updateSchedule(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="schNo") Long schNo,
            @Valid @RequestBody ReqCreateScheduleDto request
    ) {

        scheduleService.updateItem(user, schNo, request);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "일정 수정 성공", null));
    }

    @GetMapping("/holidays")
    public ResponseEntity<ResponseDto<List<HolidayDto>>> holidays(@RequestParam(name="year") int year,
                                     @RequestParam(name="month") int month) {

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "공휴일 조회 성공", scheduleService.getHolidays(year, month)));
    }
}
