package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 회의실 관리 및 예약 화면을 제공하는 View 전용 Controller
 *
 * - 회의실 관련 화면 요청을 처리하고,
 * - 화면 렌더링에 필요한 데이터를 Model에 전달하며,
 * - Thymeleaf 템플릿을 통해 화면을 제공한다.
 *
 * ※ 실제 데이터 등록·수정·삭제 처리는 API Controller에서 수행한다.

 * @author : 송현님
 * @filename : ViewMeetingRoomController
 * @since : 2025-12-16 오후 4:07 화요일
 */

@Controller
@RequestMapping("/meeting-rooms")
@RequiredArgsConstructor
public class ViewMeetingRoomController {

    // 회의실 조회
    @GetMapping
    public String meetingRoomList() {  // JWT 인증 연동 전 임시 사용
        return "reservation/meeting-rooms/meeting-room-list";
    }

    // 회의실 등록
    @GetMapping("/register")
    public String addMeetingRoom() {
        return "reservation/meeting-rooms/meeting-room-register";
    }

    // 회의실 수정
    @GetMapping("/{roomNo}/edit")
    public String editMeetingRoom(@PathVariable(name = "roomNo") Long roomNo, Model model) {
        model.addAttribute("roomNo", roomNo);
        return "reservation/meeting-rooms/meeting-room-edit";
    }

    // 회의실 예약 화면
    @GetMapping("/reservation")
    public String meetingRoomReservation() {
        return "reservation/meeting-rooms/meeting-room-reservation";
    }

    // 회의실 예약 등록 화면
    @GetMapping("/{roomNo}/reservation")
    public String meetingRoomReservation(@PathVariable(name = "roomNo") Long roomNo) {
        return "reservation/meeting-rooms/meeting-room-reservation";
    }

}
