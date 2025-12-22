package com.multi.mlpenterpriseapprovalsystem.organization.department.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 부서 화면 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewDepartmentController
 * @since : 2025. 12. 22. 월요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/departments")
public class ViewDepartmentController {

    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUser user, Model model) {
        // 필요하면 화면에 표시할 값 전달(지금 템플릿은 표시 안 하고 있음)
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");
        return "department/create";
    }
}