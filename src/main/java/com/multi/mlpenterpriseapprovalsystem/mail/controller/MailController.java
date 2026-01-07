package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;
import com.multi.mlpenterpriseapprovalsystem.mail.service.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mails")
public class MailController {

    private final MailService mailService;

    // 메일 전송 (발신자 empId는 서버 인증에서 획득)
    @PostMapping
    public ResponseEntity<ResponseDto<ResMailSendDto>> sendMail(
            @RequestBody ReqMailSendDto req,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailSendDto res = mailService.sendMail(empId, req);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "메일 전송 성공", res));
    }

    // 받은 메일함(역할 1개)
    @GetMapping("/inbox")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> inbox(
            @RequestParam("role") MailRole role,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Page<ResMailListDto> res = mailService.getInbox(empId, role, q, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "받은 메일함 조회 성공", res));
    }

    // 보낸 메일함(서버 인증 기반)
    @GetMapping("/sent")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> sent(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Page<ResMailListDto> res = mailService.getSent(empId, q, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "보낸 메일함 조회 성공", res));
    }

    // 휴지통 조회(서버 인증 기반)
    @GetMapping("/trash")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> trash(
            @PageableDefault(size = 10, sort = "deletedAt") Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Page<ResMailListDto> res = mailService.getTrash(empId, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 조회 성공", res));
    }

    // 메일 상세 조회(서버 인증 기반)
    @GetMapping("/{mailId}")
    public ResponseEntity<ResponseDto<ResMailDetailDto>> detail(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDetailDto res = mailService.getDetail(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "메일 상세 조회 성공", res));
    }

    // 읽음 처리(서버 인증 기반)
    @PatchMapping("/{mailId}/read")
    public ResponseEntity<ResponseDto<Void>> markRead(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.markAsRead(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "읽음 처리 성공", null));
    }

    // 휴지통 이동(서버 인증 기반)
    @PatchMapping("/{mailId}/trash")
    public ResponseEntity<ResponseDto<Void>> moveToTrash(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.moveToTrash(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 이동 성공", null));
    }

    // 휴지통 복원(서버 인증 기반)
    @PatchMapping("/{mailId}/restore")
    public ResponseEntity<ResponseDto<Void>> restore(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.restoreFromTrash(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 복원 성공", null));
    }

    // 완전 삭제(서버 인증 기반)
    @DeleteMapping("/{mailId}/purge")
    public ResponseEntity<ResponseDto<Void>> purge(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.purge(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "완전 삭제 성공", null));
    }

    @PatchMapping("/{mailId}/prior")
    public ResponseEntity<ResponseDto<Void>> setPrior(
            @PathVariable(name = "mailId") String mailId,
            @RequestParam("prior") boolean prior,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.setPrior(mailId, empId, prior);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "중요 메일 설정 성공", null));
    }

    @PatchMapping("/{mailId}/prior/toggle")
    public ResponseEntity<ResponseDto<Void>> togglePrior(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.togglePrior(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "중요 메일 토글 성공", null));
    }

    // 임시저장 목록
    @GetMapping("/drafts")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> drafts(
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Page<ResMailListDto> res = mailService.getDrafts(empId, q, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 목록 조회 성공", res));
    }

    // 임시저장 상세
    @GetMapping("/drafts/{mailId}")
    public ResponseEntity<ResponseDto<ResMailDetailDto>> draftDetail(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDetailDto res = mailService.getDraftDetail(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 상세 조회 성공", res));
    }

    // 임시저장 저장/수정
    @PostMapping("/drafts")
    public ResponseEntity<ResponseDto<ResMailDraftSavedDto>> saveDraft(
            @RequestBody ReqMailDraftSaveDto req,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDraftSavedDto res = mailService.saveDraft(empId, req);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 성공", res));
    }

    // 임시저장 삭제(완전삭제)
    @DeleteMapping("/drafts/{mailId}")
    public ResponseEntity<ResponseDto<Void>> deleteDraft(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.deleteDraft(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 삭제 성공", null));
    }

    // 임시저장 발송
    @PostMapping("/drafts/{mailId}/send")
    public ResponseEntity<ResponseDto<ResMailSendDto>> sendDraft(
            @PathVariable(name = "mailId") String mailId,
            @RequestBody ReqMailDraftSendDto req,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailSendDto res = mailService.sendDraft(mailId, empId, req);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 발송 성공", res));
    }
}
