package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 공유 설비 관리(조회·등록·수정) 화면을 제공하는 View 전용 Controller
 *
 * 공유 설비 관련 화면 요청을 처리하고,
 * 화면 렌더링에 필요한 데이터를 Model에 전달하며,
 * Thymeleaf 템플릿을 통해 화면을 제공한다.
 *
 * ※ 실제 데이터 등록·수정·삭제 처리는 API Controller에서 수행한다.
 *
 * @author : 송현님
 * @filename : ViewSharedEquipmentController
 * @since : 2025-12-21 오후 11:32 일요일
 */

@Controller
@RequestMapping("reservation/shared-equipment")
@RequiredArgsConstructor
public class ViewSharedEquipmentController {

    @GetMapping
    public String sharedEquipmentList() {
        return "reservation/shared-equipment/shared-equipment-list";
    }

    @GetMapping("/shared-equipment-register")
    public String addSharedEquipment() {
        return "reservation/shared-equipment/shared-equipment-register";
    }

    @GetMapping("/{eqNo}/shared-equipment-edit")
    public String editSharedEquipment(@PathVariable Long eqNo, Model model) {
        model.addAttribute("eqNo", eqNo);
        return "reservation/shared-equipment/shared-equipment-edit";
    }

    // 공유 설비 예약 조회 화면
    @GetMapping("/shared-equipment-reservation")
    public String sharedEquipmentReservation() {
        return "reservation/shared-equipment/shared-equipment-reservation";
    }
}
