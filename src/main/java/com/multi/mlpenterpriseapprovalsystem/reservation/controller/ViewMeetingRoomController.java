package com.multi.mlpenterpriseapprovalsystem.reservation.controller;

import com.multi.mlpenterpriseapprovalsystem.reservation.service.MeetingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 회의실 관리(조회·등록·수정) 화면을 제공하는 View 전용 Controller
 *
 * - 회의실 관련 화면 요청을 처리하고,
 * - 화면 렌더링에 필요한 데이터를 Model에 전달하며,
 * - Thymeleaf 템플릿을 통해 화면을 제공한다.
 *
 * ※ 실제 데이터 등록·수정·삭제 처리는 API Controller에서 수행한다.
 *
 * ※ 현재 comId는 임시로 RequestParam에서 전달받으며,
 *    JWT 인증 연동 후 SecurityContext에서 추출하도록 변경 예정이다.
 * @author : 송현님
 * @filename : ViewMeetingRoomController
 * @since : 2025-12-16 오후 4:07 화요일
 */

@Controller
@RequestMapping("/meeting-rooms")
@RequiredArgsConstructor
public class ViewMeetingRoomController {
    private final MeetingRoomService meetingRoomService;

    @GetMapping
    public String meetingRoomList() {  // JWT 인증 연동 전 임시 사용
        return "meeting-rooms/list";
    }

    @GetMapping("/regist")
    public void addMeetingRoom() {

    }

}
