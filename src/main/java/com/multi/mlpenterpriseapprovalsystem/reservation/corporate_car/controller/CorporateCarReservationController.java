package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;

import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto.ReqReservationCreateDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.service.CorporateCarReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 법인 차량 예약 API Controller
 * <p>
 * 법인 차량 예약과 관련된 사용자 행위(예약 조회·생성·취소)를 처리하는
 * REST API 전용 Controller이다.
 *
 * @author : 송현님
 * @filename : CorporateCarReservationController
 * @since : 2025-12-27 오후 10:16 토요일
 */

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CorporateCarReservationController {

    private final CorporateCarReservationService corporateCarReservationService;

    @GetMapping("/corporate-car-reservations")
    public ResponseEntity<ResponseDto<List<ResReservationListDto>>> getReservations(@AuthenticationPrincipal CustomUser user,
                                                                                    @RequestParam(required = false) String data) {
        String comId = user.getComId();
        List<ResReservationListDto> reservations =
                corporateCarReservationService.getReservations(comId, data);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, null, reservations));
    }

    @PostMapping("/corporate-car-reservations")
    public ResponseEntity<ResponseDto<List<ResReservationListDto>>> createReservation(@RequestBody ReqReservationCreateDto dto,
                                                                                      @AuthenticationPrincipal CustomUser user) {
        corporateCarReservationService.createReservation(dto, user);
        return ResponseEntity.ok().build();
    }
}






