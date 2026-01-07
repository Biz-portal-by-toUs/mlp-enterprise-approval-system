package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.ReqMailSendDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailDetailDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailListDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailSendDto;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;
import com.multi.mlpenterpriseapprovalsystem.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : MailController
 * @since : 2025-12-30 화요일
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mails")
public class MailController {

    private final MailService mailService;

    // 메일 전송
    // - senderEmpId: 로그인 사용자로 대체 예정 (지금은 파라미터로 받음)
    @PostMapping
    public ResponseEntity<ResMailSendDto> sendMail(
            @RequestParam("senderEmpId") String senderEmpId,
            @RequestBody ReqMailSendDto req
    ) {
        return ResponseEntity.ok(mailService.sendMail(senderEmpId, req));
    }

    // 받은 메일함(역할 1개)
    @GetMapping("/inbox")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> inbox(
            @AuthenticationPrincipal CustomUser user,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "받은 메일함 목록 조회 성공", mailService.getInbox(user, pageable)));
    }

    // 받은 메일함(역할 여러개) - 추후 확장용
    @GetMapping("/inbox/roles")
    public ResponseEntity<Page<ResMailListDto>> inboxByRoles(
            @RequestParam("userEmpId") String userEmpId,
            @RequestParam("roles") List<MailRole> roles,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(mailService.getInboxByRoles(userEmpId, roles, pageable));
    }

    // 보낸 메일함
    @GetMapping("/sent")
    public ResponseEntity<Page<ResMailListDto>> sent(
            @RequestParam("senderEmpId") String senderEmpId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(mailService.getSent(senderEmpId, pageable));
    }

    // 휴지통 조회
    @GetMapping("/trash")
    public ResponseEntity<Page<ResMailListDto>> trash(
            @RequestParam("userEmpId") String userEmpId,
            @PageableDefault(size = 20, sort = "deletedAt") Pageable pageable
    ) {
        return ResponseEntity.ok(mailService.getTrash(userEmpId, pageable));
    }

    // 메일 상세 조회
    @GetMapping("/{mailId}")
    public ResponseEntity<ResMailDetailDto> detail(
            @PathVariable(name = "mailId") String mailId,
            @RequestParam("viewerEmpId") String viewerEmpId
    ) {
        return ResponseEntity.ok(mailService.getDetail(mailId, viewerEmpId.trim()));
    }

    // 읽음 처리
    @PatchMapping("/{mailId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable(name = "mailId") String mailId,
            @RequestParam("userEmpId") String userEmpId
    ) {
        mailService.markAsRead(mailId, userEmpId);
        return ResponseEntity.noContent().build();
    }

    // 휴지통 이동
    @PatchMapping("/{mailId}/trash")
    public ResponseEntity<Void> moveToTrash(
            @PathVariable(name = "mailId") String mailId,
            @RequestParam("userEmpId") String userEmpId
    ) {
        mailService.moveToTrash(mailId, userEmpId);
        return ResponseEntity.noContent().build();
    }

    // 휴지통 복원
    @PatchMapping("/{mailId}/restore")
    public ResponseEntity<Void> restore(
            @PathVariable String mailId,
            @RequestParam("userEmpId") String userEmpId
    ) {
        mailService.restoreFromTrash(mailId, userEmpId);
        return ResponseEntity.noContent().build();
    }

    // 완전 삭제
    @DeleteMapping("/{mailId}/purge")
    public ResponseEntity<Void> purge(
            @PathVariable String mailId,
            @RequestParam("userEmpId") String userEmpId
    ) {
        mailService.purge(mailId, userEmpId);
        return ResponseEntity.noContent().build();
    }
}