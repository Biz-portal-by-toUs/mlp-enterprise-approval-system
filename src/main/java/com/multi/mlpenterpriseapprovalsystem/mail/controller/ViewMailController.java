package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import lombok.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

/**
 * 메일 화면(View) 라우팅 컨트롤러
 *
 * @author : 정종원
 * @filename : ViewMailController
 * @since : 2026-01-05 월요일
 */

@Controller
@RequestMapping("/mail")
@RequiredArgsConstructor
public class ViewMailController {

    // 기본 받은메일함
    @GetMapping
    public String mailHome() {
        return "redirect:/mail/received";
    }

    // 받은 메일함
    @GetMapping("/received")
    public String received() {
        // templates/mail/received.html
        return "mail/received";
    }

    // 보낸 메일함
    @GetMapping("/sent")
    public String sent() {
        // templates/mail/sent.html
        return "mail/sent";
    }

    // 메일 쓰기
    @GetMapping("/write")
    public String write() {
        // templates/mail/write.html
        return "mail/write";
    }

    // 휴지통
    @GetMapping("/trash")
    public String trash() {
        // templates/mail/trash.html
        return "mail/trash";
    }

    // 상세
    @GetMapping("/{mailId}")
    public String detail(@PathVariable String mailId, Model model) {
        model.addAttribute("mailId", mailId);
        return "mail/detail";
    }

    // 임시보관함
    @GetMapping("/drafts")
    public String drafts() {
        return "mail/drafts";
    }
}
