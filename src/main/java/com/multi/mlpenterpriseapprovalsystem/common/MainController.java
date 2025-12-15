package com.multi.mlpenterpriseapprovalsystem.common;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping({"/main" ,"/"})
    public String main(Model model) {

        // 메인 레이아웃 페이지로 이동
        return "main";
    }
}
