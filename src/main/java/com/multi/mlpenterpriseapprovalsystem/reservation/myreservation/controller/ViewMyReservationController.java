package com.multi.mlpenterpriseapprovalsystem.reservation.myreservation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 내 예약 조회 화면(View) 컨트롤러
 *
 * "내 예약 조회" 페이지를 보여주기 위한 컨트롤러다.
 * API(JSON)를 내려주는 RestController가 아니라,
 * 사용자가 브라우저에서 접근했을 때 템플릿(Thymeleaf)을 반환한다.
 *
 * @author : 송현님
 * @filename : ViewMyReservationController
 * @since : 2025-12-28 오후 10:28 일요일
 */

@Controller
@RequestMapping
public class ViewMyReservationController {

    @GetMapping("/my-reservations")
    public String page() {
        return "reservation/myreservation/my-reservations";
    }
}
