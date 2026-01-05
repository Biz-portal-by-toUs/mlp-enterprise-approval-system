package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    /** ✅ 휴지통 */
    @GetMapping("/cloud/trash")
    public String trash(@RequestParam(defaultValue = "DEPT") FolderScope scope, Model model) {
        // sidebar 탭 active 표시용 (dept/prvt)
        String active = (scope == FolderScope.PRVT) ? "prvt" : "dept";

        model.addAttribute("active", active);
        model.addAttribute("scope", active);      // (JS에서 dept/prvt 문자열로 쓰는 용도)
        model.addAttribute("trashScope", scope);  // (REST 호출할 때 DEPT/PRVT로 쓰는 용도)

        return "cloud/trash";
    }
}

