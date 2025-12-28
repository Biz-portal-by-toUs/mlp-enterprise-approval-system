package com.multi.mlpenterpriseapprovalsystem.reservation.my.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.my.dto.ResMyReservationDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.my.service.MyReservationService;
import lombok.RequiredArgsConstructor;
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
 * 내 예약 조회 API 컨트롤러
 *
 * 사용자가 지정한 기간(from~to)과 선택한 예약 대상(domain)에 따라
 * "내 예약 목록"을 조회해서 JSON으로 반환한다.
 *
 * @author : 송현님
 * @filename : MyReservationController
 * @since : 2025-12-28 오후 10:33 일요일
 */

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MyReservationController {

    private final MyReservationService myReservationService;

    @GetMapping("/my-reservations")
    public ResponseEntity<ResponseDto<List<ResMyReservationDto>>> getMyReservations(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("to")   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "domain", required = false, defaultValue = "ALL") String domain,
            @AuthenticationPrincipal CustomUser user
    ) {
        List<ResMyReservationDto> rows = myReservationService.getMyReservations(user, startDate, endDate, domain);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "내 예약 조회 성공", rows));
    }

}
