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
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class ViewAuthController {

    // 회사 회원가입 화면 (GET)
    @GetMapping("/companies/signup")
    public String companySignupPage() {
        // templates/company/common/signup.html
        return "company/common/signup";
    }

    // 회사 로그인 화면 (GET)
    @GetMapping("/companies/login")
    public String companyLoginPage() {
        return "company/common/login"; // templates/company/common/login.html
    }

    // 사원 로그인 화면 (GET)
    @GetMapping("/employee/login")
    public String employeeLoginPage() {
        return "employee/common/login"; // templates/employee/common/login.html
    }
}
