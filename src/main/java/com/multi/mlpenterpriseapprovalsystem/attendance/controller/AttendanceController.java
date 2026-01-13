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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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

    // 수정가능한 내 근태 조회
    @GetMapping("/me/modifiable")
    public ResponseEntity<ResponseDto<List<ResAttendanceDto>>> getMyModifiableAttendances(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "ATTETYPE") String atteType) {
        String comId = customUser.getComId();
        String myEmpId = customUser.getUsername();

        List<ResAttendanceDto> resAttendanceDtos = attendanceService.getMyModifiableAttendances(comId, myEmpId, atteType);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "내 수정 가능한 근태 조회 성공", resAttendanceDtos));
    }


    // 취소가능한 내 근태 조회
    @GetMapping("/me/cancelable")
    public ResponseEntity<ResponseDto<List<ResAttendanceDto>>> getMyCancelableAttendances(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "ATTETYPE") String atteType) {
        String comId = customUser.getComId();
        String myEmpId = customUser.getUsername();

        List<ResAttendanceDto> resAttendanceDtos = attendanceService.getMyCancelableAttendances(comId, myEmpId, atteType);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "내 취소 가능한 근태 조회 성공", resAttendanceDtos));
    }

    // 나를 대직자로 설정한 모든 활성 근태 목록 조회
    @GetMapping("/me/delegate-periods")
    public ResponseEntity<ResponseDto<List<ResAttendanceDto>>> getMyDelegatePeriods(
            @AuthenticationPrincipal CustomUser customUser) {

        // 나를 대직자로 설정한 모든 활성 근태 목록 조회
        List<ResAttendanceDto> periods = attendanceService.getPeriodsWhereIAmDelegate(customUser.getUsername());
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "대직 예정 기간 조회 성공", periods));
    }


    // 내 근태 중복 체크
    @GetMapping("/me/overlap-check")
    public ResponseEntity<ResponseDto<List<ResAttendanceDto>>> checkMyAttendanceOverlap(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "startAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startAt,
            @RequestParam(name = "endAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endAt,
            @RequestParam(name = "excludeAtteNo", required = false) Long excludeAtteNo) {

        LocalDateTime normalizedStart = startAt.toLocalDate().atStartOfDay();
        LocalDateTime normalizedEnd = endAt.toLocalDate().atTime(23, 59, 59);

        List<ResAttendanceDto> overlaps = attendanceService.getMyAttendanceOverlapList(
                customUser.getComId(),
                customUser.getUsername(),
                normalizedStart,
                normalizedEnd,
                excludeAtteNo
        );

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "중복 여부 확인 성공", overlaps));
    }
}
