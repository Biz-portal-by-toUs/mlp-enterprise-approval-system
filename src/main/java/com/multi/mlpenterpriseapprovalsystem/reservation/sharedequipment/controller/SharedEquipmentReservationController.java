package com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;

import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.dto.ReqReservationCreateDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.service.SharedEquipmentReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 공유 설비 예약 API Controller

 * 공유 설비 예약과 관련된 사용자 행위(예약 조회·생성·취소)를 처리하는
 * REST API 전용 Controller이다.
 *
 * @author : 송현님
 * @filename : SharedEquipmentReservationController
 * @since : 2025-12-28 오후 4:49 일요일
 */

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SharedEquipmentReservationController {

    private final SharedEquipmentReservationService sharedEquipmentReservationService;

    @GetMapping("/shared-equipment-reservations")
    public ResponseEntity<ResponseDto<List<ResReservationListDto>>> getReservations(@AuthenticationPrincipal CustomUser user,
                                                                                    @RequestParam(required = false, name = "data") String data) {

        String comId = user.getComId();
        List<ResReservationListDto> reservations =
                sharedEquipmentReservationService.getReservations(comId, data);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, null, reservations));

    }

    @PostMapping("/shared-equipment-reservations")
    public ResponseEntity<ResponseDto<List<ResReservationListDto>>> createReservation(@RequestBody ReqReservationCreateDto dto,
                                                                                      @AuthenticationPrincipal CustomUser user) {
        sharedEquipmentReservationService.createReservation(dto, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/shared-equipment-reservations/{resvNo}")
    public ResponseEntity<Void> deleteReservation(@PathVariable(name = "resvNo") Long resvNo,
                                                  @AuthenticationPrincipal CustomUser user) {

        sharedEquipmentReservationService.deleteReservation(resvNo, user);
        return ResponseEntity.noContent().build();   // 204
    }
}

