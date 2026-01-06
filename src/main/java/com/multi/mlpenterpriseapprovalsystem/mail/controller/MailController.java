package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;
import com.multi.mlpenterpriseapprovalsystem.mail.service.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.*;
import org.springframework.http.*;
import org.springframework.security.core.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mails")
public class MailController {

    private final MailService mailService;

    // 메일 전송 (발신자 empId는 서버 인증에서 획득)
    @PostMapping
    public ResponseEntity<ResMailSendDto> sendMail(
            @RequestBody ReqMailSendDto req,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        return ResponseEntity.ok(mailService.sendMail(empId, req));
    }

    // 받은 메일함(역할 1개)
    @GetMapping("/inbox")
    public ResponseEntity<Page<ResMailListDto>> inbox(
            @RequestParam("role") MailRole role,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        return ResponseEntity.ok(mailService.getInbox(empId, role, q, pageable));
    }

    // 보낸 메일함(서버 인증 기반)
    @GetMapping("/sent")
    public ResponseEntity<Page<ResMailListDto>> sent(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        return ResponseEntity.ok(mailService.getSent(empId, q, pageable));
    }

    // 휴지통 조회(서버 인증 기반)
    @GetMapping("/trash")
    public ResponseEntity<Page<ResMailListDto>> trash(
            @PageableDefault(size = 20, sort = "deletedAt") Pageable pageable,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        return ResponseEntity.ok(mailService.getTrash(empId, pageable));
    }

    // 메일 상세 조회(서버 인증 기반)
    @GetMapping("/{mailId}")
    public ResponseEntity<ResMailDetailDto> detail(
            @PathVariable String mailId,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        return ResponseEntity.ok(mailService.getDetail(mailId, empId));
    }

    // 읽음 처리(서버 인증 기반)
    @PatchMapping("/{mailId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable String mailId,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        mailService.markAsRead(mailId, empId);
        return ResponseEntity.noContent().build();
    }

    // 휴지통 이동(서버 인증 기반)
    @PatchMapping("/{mailId}/trash")
    public ResponseEntity<Void> moveToTrash(
            @PathVariable String mailId,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        mailService.moveToTrash(mailId, empId);
        return ResponseEntity.noContent().build();
    }

    // 휴지통 복원(서버 인증 기반)
    @PatchMapping("/{mailId}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable String mailId,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        mailService.restoreFromTrash(mailId, empId);
        return ResponseEntity.noContent().build();
    }

    // 완전 삭제(서버 인증 기반)
    @DeleteMapping("/{mailId}/purge")
    public ResponseEntity<Void> purge(
            @PathVariable String mailId,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        mailService.purge(mailId, empId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{mailId}/prior")
    public ResponseEntity<Void> setPrior(
            @PathVariable String mailId,
            @RequestParam("prior") boolean prior,
            Authentication authentication
    ) {
        String empId = authentication.getName();
        mailService.setPrior(mailId, empId, prior);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{mailId}/prior/toggle")
    public ResponseEntity<Void> togglePrior(@PathVariable String mailId, Authentication authentication) {
        String empId = authentication.getName();
        mailService.togglePrior(mailId, empId);
        return ResponseEntity.noContent().build();
    }
}
