package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ReqReservationCreateDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto.ResReservationListDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.service.MeetingRoomReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 회의실 예약 API Controller
 *
 * 회의실 예약과 관련된 사용자 행위(예약 조회·생성·취소)를 처리하는
 * REST API 전용 Controller이다.
 *
 * @author : 송현님
 * @filename : MeetingRoomReservationController
 * @since : 2025-12-22 오후 2:27 월요일
 */

@RestController
@Slf4j
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MeetingRoomReservationController {

    private final MeetingRoomReservationService meetingRoomReservationService;

    @GetMapping("/meeting-room-reservations")
    public ResponseEntity<ResponseDto<List<ResReservationListDto>>> getReservations(@AuthenticationPrincipal CustomUser user,
                                                                                    @RequestParam(required = false) String date) {  // 한 페이지에서 보여줄 데이터 개수

        String comId = user.getComId();
        List<ResReservationListDto> reservations =
                meetingRoomReservationService.getReservations(comId, date);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, null, reservations));
    }

    @PostMapping("/meeting-room-reservations")
    public ResponseEntity<ResponseDto<List<ResReservationListDto>>> createReservation(@RequestBody ReqReservationCreateDto dto,
                                                                                      @AuthenticationPrincipal CustomUser user) {
        meetingRoomReservationService.createReservation(dto, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/meeting-room-reservations/{resvNo}")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long resvNo,
            @AuthenticationPrincipal CustomUser user) {

        meetingRoomReservationService.deleteReservation(resvNo, user);
        return ResponseEntity.noContent().build();   // 204
    }
}
