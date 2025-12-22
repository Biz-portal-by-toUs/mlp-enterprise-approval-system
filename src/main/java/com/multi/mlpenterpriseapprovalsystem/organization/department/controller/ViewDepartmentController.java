package com.multi.mlpenterpriseapprovalsystem.organization.department.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    private final DepartmentRepository departmentRepository;

    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUser user, Model model) {
        // 필요하면 화면에 표시할 값 전달(지금 템플릿은 표시 안 하고 있음)
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");
        return "department/create";
    }

    @GetMapping("/list")
    public String listPage(@AuthenticationPrincipal CustomUser user, Model model) {
        // 필요하면 화면에 표시할 값 전달(지금 템플릿은 표시 안 하고 있음)
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");
        return "department/list";
    }

    @GetMapping("/{depNo}/edit")
    public String editPage(
            @PathVariable(name = "depNo") Long depNo,
            @AuthenticationPrincipal CustomUser user, // 로그인한 유저 정보를 바로 가져옴
            Model model
    ) {
        // 1. 부서 정보 조회
        Department department = departmentRepository.findById(depNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // 2. 모델에 부서 정보 담기
        model.addAttribute("depNo", department.getDepNo());
        model.addAttribute("depId", department.getDepId());
        model.addAttribute("depName", department.getDepName());

        // 3. 로그인한 유저 정보가 필요하다면?
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");

        return "department/update";
    }
}