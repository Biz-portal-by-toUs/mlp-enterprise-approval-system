package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/mail")
@RequiredArgsConstructor
public class ViewMailController {

    @GetMapping
    public String mailHome() {
        return "redirect:/mail/received";
    }

    @GetMapping("/received")
    public String received() {
        return "mail/received";
    }

    @GetMapping("/sent")
    public String sent() {
        return "mail/sent";
    }

    @GetMapping("/write")
    public String write() {
        return "mail/write";
    }

    @GetMapping("/trash")
    public String trash() {
        return "mail/trash";
    }

    // 상세
    @GetMapping("/{mailNo}")
    public String detail(@PathVariable Long mailNo, Model model) {
        model.addAttribute("mailNo", mailNo);
        return "mail/detail";
    }

    @GetMapping("/drafts")
    public String drafts() {
        return "mail/drafts";
    }
}
