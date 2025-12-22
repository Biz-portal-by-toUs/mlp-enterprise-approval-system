package com.multi.mlpenterpriseapprovalsystem.organization.positions.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 직급 조회 페이지 반환 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewPositionsController
 * @since : 2025. 12. 22. 월요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/positions")
public class ViewPositionsController {

    @GetMapping("/list")
    public String listPage(@AuthenticationPrincipal CustomUser user, Model model) {
        // 필요하면 화면에 표시할 값 전달(지금 템플릿은 표시 안 하고 있음)
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");
        return "positions/list";
    }
}
