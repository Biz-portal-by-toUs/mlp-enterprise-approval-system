package com.multi.mlpenterpriseapprovalsystem.employee.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 사원 관련 뷰 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewEmployeeController
 * @since : 2025. 12. 29. 월요일
 */
@Controller
@RequestMapping("/employees")
@RequiredArgsConstructor
public class ViewEmployeeController {

    @GetMapping
    public String mainPage(@AuthenticationPrincipal CustomUser user, Model model) {

        String comId = (user != null ? user.getComId() : null);

        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", comId != null ? comId : "");
        return "employeeMainPage";
    }

    @GetMapping("/me")
    public String me(@AuthenticationPrincipal CustomUser user, Model model) {
        String comId = (user != null ? user.getComId() : null);
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", comId != null ? comId : "");
        return "employee/me";
    }
}
