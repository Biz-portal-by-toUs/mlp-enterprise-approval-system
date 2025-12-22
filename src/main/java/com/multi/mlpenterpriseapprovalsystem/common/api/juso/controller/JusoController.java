package com.multi.mlpenterpriseapprovalsystem.common.api.juso.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 주소 api 활용하는 컨트롤러
 *
 * @author : 권지영
 * @filename : JusoController
 * @since : 2025. 12. 21. 일요일
 */
@Controller
@RequestMapping("/auth/juso")
public class JusoController {

    @Value("${juso.api.key}")
    private String apiKey;

    @GetMapping("/popup")
    public String jusoPopup(Model model) {
        model.addAttribute("apiKey", apiKey);   // ✅ 이거 필수
        model.addAttribute("inputData", null);  // ✅ 첫 진입은 null로
        return "juso/jusoPopup";
    }

    @PostMapping("/popup")
    public String jusoPopupPost(HttpServletRequest request, Model model) {
        model.addAttribute("apiKey", apiKey);                 // ✅ POST에서도 유지
        model.addAttribute("inputData", request.getParameterMap());
        return "juso/jusoPopup";
    }
}