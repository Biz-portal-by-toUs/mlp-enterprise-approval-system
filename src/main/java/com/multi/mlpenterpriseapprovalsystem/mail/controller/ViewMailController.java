package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.mail.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/mail")
@RequiredArgsConstructor
public class ViewMailController {

    private final MailService mailService;

    @GetMapping
    public String mailHome() {
        return "redirect:/mail/received";
    }

    @GetMapping("/received")
    public String received(Model model) {
        model.addAttribute("sidebarKey", "received");
        return "mail/received";
    }

    @GetMapping("/sent")
    public String sent(Model model) {
        model.addAttribute("sidebarKey", "sent");
        return "mail/sent";
    }

    @GetMapping("/self")
    public String self(Model model) {
        model.addAttribute("sidebarKey", "self");
        return "mail/self";
    }

    @GetMapping("/drafts")
    public String drafts(Model model) {
        model.addAttribute("sidebarKey", "drafts");
        return "mail/drafts";
    }

    @GetMapping("/trash")
    public String trash(Model model) {
        model.addAttribute("sidebarKey", "trash");
        return "mail/trash";
    }

    @GetMapping("/write")
    public String write(Model model) {
        // sidebar를 mail로 유지하기 위해 received 기본값
        model.addAttribute("sidebarKey", "received");
        return "mail/write";
    }

    // 상세
    @GetMapping("/{mailNo}")
    public String detail(
            @PathVariable Long mailNo,
            @RequestParam(name = "box", required = false) String box,
            Model model
    ) {
        model.addAttribute("mailNo", mailNo);

        // 2번 방안 핵심: 서버에서 sidebarKey 결정
        String sidebarKey = normalizeBox(box);
        if (sidebarKey == null) sidebarKey = "received"; // 기본값(원하면 trash 등으로 변경 가능)

        model.addAttribute("sidebarKey", sidebarKey);
        return "mail/detail";
    }

    private String normalizeBox(String box){
        if (box == null) return null;
        String v = box.trim().toLowerCase();
        return switch (v){
            case "received", "sent", "self", "drafts", "trash" -> v;
            default -> null;
        };
    }

    @ModelAttribute
    public void commonMailSidebarCounts(Model model,
                                        @AuthenticationPrincipal CustomUser user) {
        if (user == null) return;
        String empId = user.getUsername();

        long unread = mailService.countUnreadInbox(empId); // 아래 3) 참고
        model.addAttribute("unreadInboxCount", unread);
    }
}