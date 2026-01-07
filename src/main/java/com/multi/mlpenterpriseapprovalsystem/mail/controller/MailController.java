package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 메일 Rest API 컨트롤러
 *
 * @author : 정종원
 * @filename : MailController
 * @since : 2026-01-05 월요일
 */

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

    // 받은 메일함
    @GetMapping("/inbox")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> inbox(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Page<ResMailListDto> res = mailService.getInbox(empId, keyword, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "받은 메일함 조회 성공", res));
    }

    // 보낸 메일함
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

    // 휴지통 조회
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

    // =========================
    // ✅ mailNo 기준 상세/상태 API
    // =========================

    // 메일 상세 조회 (mailNo 기준)
    @GetMapping("/{mailNo}")
    public ResponseEntity<ResponseDto<ResMailDetailDto>> detail(
            @PathVariable Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDetailDto res = mailService.getDetail(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "메일 상세 조회 성공", res));
    }

    // 읽음 처리 (mailNo 기준)
    @PatchMapping("/{mailNo}/read")
    public ResponseEntity<ResponseDto<Void>> markRead(
            @PathVariable Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.markAsRead(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "읽음 처리 성공", null));
    }

    // 휴지통 이동 (mailNo 기준)
    @PatchMapping("/{mailNo}/trash")
    public ResponseEntity<ResponseDto<Void>> moveToTrash(
            @PathVariable Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.moveToTrash(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 이동 성공", null));
    }

    // 휴지통 복원 (mailNo 기준)
    @PatchMapping("/{mailNo}/restore")
    public ResponseEntity<ResponseDto<Void>> restore(
            @PathVariable Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.restoreFromTrash(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 복원 성공", null));
    }

    // 완전 삭제 (mailNo 기준)
    @DeleteMapping("/{mailNo}/purge")
    public ResponseEntity<ResponseDto<Void>> purge(
            @PathVariable Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.purge(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "완전 삭제 성공", null));
    }

    // 중요 메일 설정 (mailNo 기준)
    @PatchMapping("/{mailNo}/prior")
    public ResponseEntity<ResponseDto<Void>> setPrior(
            @PathVariable Long mailNo,
            @RequestParam("prior") boolean prior,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.setPrior(mailNo, empId, prior);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "중요 메일 설정 성공", null));
    }

    // 중요 메일 토글 (mailNo 기준)
    @PatchMapping("/{mailNo}/prior/toggle")
    public ResponseEntity<ResponseDto<Void>> togglePrior(
            @PathVariable Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.togglePrior(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "중요 메일 토글 성공", null));
    }

    // =========================
    // (선택) 🔻mailId 기반 호환 API (나중에 제거)
    // =========================

    @Deprecated
    @GetMapping("/by-id/{mailId}")
    public ResponseEntity<ResponseDto<ResMailDetailDto>> detailByMailId(
            @PathVariable String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDetailDto res = mailService.getDetailByMailId(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "메일 상세 조회 성공(by mailId)", res));
    }

    @Deprecated
    @PatchMapping("/by-id/{mailId}/read")
    public ResponseEntity<ResponseDto<Void>> markReadByMailId(
            @PathVariable String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.markAsReadByMailId(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "읽음 처리 성공(by mailId)", null));
    }

    // ===== drafts =====

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

    @GetMapping("/drafts/{mailId}")
    public ResponseEntity<ResponseDto<ResMailDetailDto>> draftDetail(
            @PathVariable String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDetailDto res = mailService.getDraftDetail(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 상세 조회 성공", res));
    }

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

    @DeleteMapping("/drafts/{mailId}")
    public ResponseEntity<ResponseDto<Void>> deleteDraft(
            @PathVariable String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.deleteDraft(mailId, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 삭제 성공", null));
    }

    @PostMapping("/drafts/{mailId}/send")
    public ResponseEntity<ResponseDto<ResMailSendDto>> sendDraft(
            @PathVariable String mailId,
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