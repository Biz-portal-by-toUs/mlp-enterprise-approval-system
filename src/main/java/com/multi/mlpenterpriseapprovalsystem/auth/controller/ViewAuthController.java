package com.multi.mlpenterpriseapprovalsystem.auth.controller;


/**
 * 인증 파트 프론트 컨트롤러
 *
 * @author : 권지영
 * @filename : FrontAuthController
 * @since : 2025. 12. 18. 목요일
 */

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewAuthController {

    // 회사 로그인 화면 (GET)
    @GetMapping("/auth/companies/login")
    public String companyLoginPage() {
        return "company/common/login"; // templates/company/common/login.html
    }

    // 사원 로그인 화면 (GET)
    @GetMapping("/auth/employee/login")
    public String employeeLoginPage() {
        return "employee/common/login"; // templates/employee/common/login.html
    }
}
