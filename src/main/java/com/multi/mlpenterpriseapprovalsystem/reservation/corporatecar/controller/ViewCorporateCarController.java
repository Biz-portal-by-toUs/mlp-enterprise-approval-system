package com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 법인차량 관리(조회·등록·수정) 화면을 제공하는 View 전용 Controller
 *
 * - 법인차량 관련 화면 요청을 처리하고,
 * - 화면 렌더링에 필요한 데이터를 Model에 전달하며,
 * - Thymeleaf 템플릿을 통해 화면을 제공한다.
 *
 * ※ 실제 데이터 등록·수정·삭제 처리는 API Controller에서 수행한다.
 *
 * @author : 송현님
 * @filename : ViewCorporateCarController
 * @since : 2025-12-20 오후 5:31 토요일
 */

@Controller
@RequestMapping("/corporate-cars")
@RequiredArgsConstructor
public class ViewCorporateCarController {

    @GetMapping
    public String corporateCarList() {
        return "reservation/corporate-cars/corporate-car-list";
    }

    @GetMapping("/register")
    public String addCorporateCar() {
        return "reservation/corporate-cars/corporate-car-register";
    }

    @GetMapping("/{carNo}/edit")
    public String editCorporateCar(@PathVariable(name = "carNo") Long carNo, Model model) {
        model.addAttribute("carNo", carNo);
        return "reservation/corporate-cars/corporate-car-edit";
    }

    // 법인 차량 예약 조회 화면
    @GetMapping("/reservation")
    public String corporateCarReservation() {
        return "reservation/corporate-cars/corporate-car-reservation";
    }

    // 법인 차량 예약 등록 화면
    @GetMapping("/{carNo}/reservation")
    public String corporateCarReservation(@PathVariable(name = "carNo") Long carNo) {
        return "reservation/corporate-cars/corporate-car-reservation";
    }

}
