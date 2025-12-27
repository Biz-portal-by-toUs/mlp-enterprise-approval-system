package com.multi.mlpenterpriseapprovalsystem.employee.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.repository.PositionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 사원 관련 프론트엔드 연결 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewEmployeeController
 * @since : 2025. 12. 23. 화요일
 */
@Controller
@RequestMapping("/admin/employees")
@RequiredArgsConstructor
public class ViewEmployeeController {

    private final DepartmentRepository departmentRepository;
    private final PositionsRepository positionsRepository;

    @GetMapping("/list")
    public String employeeListPage(@AuthenticationPrincipal CustomUser user, Model model) {

        String comId = (user != null ? user.getComId() : null);

        // 상단바 정보 (null이면 빈값)
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", comId != null ? comId : "");

        // ✅ 드롭다운 데이터는 로그인(=user 존재)일 때만 조회
        if (comId != null && !comId.isBlank()) {
            model.addAttribute("departments", departmentRepository.findAllByCompany_ComId(comId));
            model.addAttribute("positionsList", positionsRepository.findAllByCompany_ComId(comId));
        } else {
            // 템플릿에서 null 처리 싫으면 빈 리스트로
            model.addAttribute("departments", java.util.Collections.emptyList());
            model.addAttribute("positionsList", java.util.Collections.emptyList());
        }

        return "/employee/list";
    }

    @GetMapping("/{empNo}")
    public String employeeDetailPage(@PathVariable(name="empNo") Long empNo, Model model) {
        model.addAttribute("empNo", empNo);
        return "employee/detail";
    }
}
