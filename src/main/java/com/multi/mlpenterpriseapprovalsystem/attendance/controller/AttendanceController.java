package com.multi.mlpenterpriseapprovalsystem.attendance.controller;

import com.multi.mlpenterpriseapprovalsystem.attendance.dto.res.ResAttendanceDto;
import com.multi.mlpenterpriseapprovalsystem.attendance.service.AttendanceService;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 근태 컨트롤러
 *
 * @author : 이지헌
 * @filename : AttendanceController
 * @since : 25. 12. 31. 수요일
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 내 모든 근태 정보 조회
//    @GetMapping("/me")
//    public ResponseEntity<ResponseDto<List<ResAttendanceDto>>> getMyAttendances(@AuthenticationPrincipal CustomUser customUser) {
//
//        List<ResAttendanceDto> resAttendanceDtos = attendanceService.getMyAttendances(customUser.getComId(), customUser.getUsername());
//
//        return ResponseEntity
//                .ok()
//                .body(new ResponseDto<>(HttpStatus.OK, "내 근태 정보 조회 성공", resAttendanceDtos));
//    }

    // 내 휴가 정보 조회
    @GetMapping("/me/vacations")
    public ResponseEntity<ResponseDto<List<ResAttendanceDto>>> getMyVacations(@AuthenticationPrincipal CustomUser customUser) {

        List<ResAttendanceDto> resAttendanceDtos = attendanceService.getMyVacations(customUser.getComId(), customUser.getUsername());

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "내 휴가 정보 조회 성공", resAttendanceDtos));
    }

    // 내 출장 정보 조회
    @GetMapping("/me/business-trips")
    public ResponseEntity<ResponseDto<List<ResAttendanceDto>>> getMyBusinessTrips(@AuthenticationPrincipal CustomUser customUser) {

        List<ResAttendanceDto> resAttendanceDtos = attendanceService.getMyBusinessTrips(customUser.getComId(), customUser.getUsername());

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "내 출장 정보 조회 성공", resAttendanceDtos));
    }


    /**
     * 근태 식별자로 근태 정보 조회
     */
    @GetMapping("/{atteNo}")
    public ResponseEntity<ResponseDto<ResAttendanceDto>> getAttendanceByAtteNo(
            @PathVariable(name = "atteNo") Long atteNo,
            @AuthenticationPrincipal CustomUser customUser) {

        ResAttendanceDto attendance = attendanceService.getAttendanceByAtteNo(
                customUser.getComId(),
                customUser.getUsername(),
                atteNo
        );

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "근태 정보 조회 성공", attendance));
    }

    // 전체 회사 근태 페이지별 조회
    @GetMapping("")
    public ResponseEntity<ResponseDto<Page<ResAttendanceDto>>> getAllAttendances(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ResAttendanceDto> resAttendanceDtos = attendanceService.getAllAttendances(customUser.getComId(), pageable);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "전체 근태 정보 조회 성공", resAttendanceDtos));
    }
}
