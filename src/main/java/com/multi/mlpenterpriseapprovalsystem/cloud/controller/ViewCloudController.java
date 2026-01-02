package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 클라우드(공유/개인 파일함) 화면을 띄워주는 뷰 컨트롤러
 *
 * @author : 송현님
 * @filename : ViewFolderController
 * @since : 2025-12-29 오후 5:37 월요일
 */
@Controller
public class ViewCloudController {

    /** 기본 진입: 공유함으로 */
    @GetMapping("/cloud")
    public String root() {
        return "redirect:/cloud/dept";
    }

    /** 공유함 */
    @GetMapping("/cloud/dept")
    public String dept(Model model) {
        model.addAttribute("active", "dept"); // sidebar active 표시용
        model.addAttribute("scope", "dept");  // JS 초기 scope용
        return "cloud/cloud";
    }

    /** 개인함 */
    @GetMapping("/cloud/prvt")
    public String prvt(Model model) {
        model.addAttribute("active", "prvt");
        model.addAttribute("scope", "prvt");
        return "cloud/cloud";
    }
}

