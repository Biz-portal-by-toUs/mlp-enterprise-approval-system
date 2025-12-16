package com.multi.mlpenterpriseapprovalsystem.reservation.controller;

import com.multi.mlpenterpriseapprovalsystem.reservation.dto.MeetingRoomResponseDto;
import com.multi.mlpenterpriseapprovalsystem.reservation.register.domain.MeetingRoom;
import com.multi.mlpenterpriseapprovalsystem.reservation.reserve.domain.MeetingRoomReservation;
import com.multi.mlpenterpriseapprovalsystem.reservation.service.MeetingRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Please explain the class!!!
 * 회의실 관리 기능에 대한 REST API 요청을 처리하는 Controller.
 *
 * - 회의실 관련 데이터 조회 요청을 처리한다.
 * - Service 계층을 통해 비즈니스 로직을 수행한다.
 * - 처리 결과를 JSON 형태의 Response DTO로 반환한다.
 *
 * ※ 현재 comId는 임시로 RequestParam에서 전달받으며,
 *    JWT 인증 연동 후 Access Token에서 추출하도록 변경 예정이다.
 *
 * @author : 송현님
 * @filename : MeetingRoomController
 * @since : 2025-12-16 오후 2:47 화요일
 */

@RestController
@Slf4j
@RequestMapping("/api/v1")
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    public MeetingRoomController(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }

    @GetMapping("/meeting-rooms")
    public List<MeetingRoomResponseDto> getMeetingRooms(@RequestParam String comId) {  // 임시
        return  meetingRoomService.getMeetingRooms(comId).stream()
                .map(room -> MeetingRoomResponseDto.builder()
                        .roomNo(room.getRoomNo())
                        .comId(room.getCompany().getComId())
                        .roomName(room.getRoomName())
                        .capacity(room.getCap())
                        .location(room.getLoc())
                        .imageUrl(room.getImgUrl())
                        .equipList(room.getEquipList())
                        .note(room.getNote())
                        .build())
                .toList();
    }

}
