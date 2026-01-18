package com.multi.mlpenterpriseapprovalsystem.mail.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.*;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mails")
@Slf4j
public class MailController {

    private final MailService mailService;

    @PostMapping
    public ResponseEntity<ResponseDto<ResMailSendDto>> sendMail(
            @RequestBody ReqMailSendDto req,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailSendDto res = mailService.sendMail(empId, req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "메일 전송 성공", res));
    }

    // 답신 payload (추가)
    @GetMapping("/{mailNo}/reply")
    public ResponseEntity<ResponseDto<ResMailReplyPayloadDto>> replyPayload(
            @PathVariable(name = "mailNo") Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailReplyPayloadDto res = mailService.getReplyPayload(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "답신 payload 조회 성공", res));
    }

    // 받은 메일함
    @GetMapping("/inbox")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> inbox(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "priorOnly", required = false) Boolean priorOnly,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        Page<ResMailListDto> res = Boolean.TRUE.equals(priorOnly)
                ? mailService.getPriorInbox(empId, keyword, from, to, pageable)
                : mailService.getInbox(empId, keyword, from, to, pageable);

        String msg = Boolean.TRUE.equals(priorOnly)
                ? "중요 받은 메일함 조회 성공"
                : "받은 메일함 조회 성공";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, msg, res));
    }

    @GetMapping("/sent")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> sent(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "priorOnly", required = false) Boolean priorOnly,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        Page<ResMailListDto> res = Boolean.TRUE.equals(priorOnly)
                ? mailService.getPriorSent(empId, keyword, from, to, pageable)
                : mailService.getSent(empId, keyword, from, to, pageable);

        String msg = Boolean.TRUE.equals(priorOnly)
                ? "중요 보낸 메일함 조회 성공"
                : "보낸 메일함 조회 성공";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, msg, res));
    }

    @GetMapping("/trash")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> trash(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Page<ResMailListDto> res = mailService.getTrash(empId, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 조회 성공", res));
    }

    @GetMapping("/{mailNo}")
    public ResponseEntity<ResponseDto<ResMailDetailDto>> detail(
            @PathVariable(name = "mailNo") Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDetailDto res = mailService.getDetail(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "메일 상세 조회 성공", res));
    }

    @PatchMapping("/{mailNo}/read")
    public ResponseEntity<ResponseDto<Void>> markRead(
            @PathVariable(name = "mailNo") Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.markAsRead(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "읽음 처리 성공", null));
    }

    @PatchMapping("/{mailNo}/trash")
    public ResponseEntity<ResponseDto<Void>> moveToTrash(
            @PathVariable(name = "mailNo") Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.moveToTrash(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 이동 성공", null));
    }

    @PatchMapping("/{mailNo}/restore")
    public ResponseEntity<ResponseDto<Void>> restore(
            @PathVariable(name = "mailNo") Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.restoreFromTrash(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "휴지통 복원 성공", null));
    }

    @DeleteMapping("/{mailNo}/purge")
    public ResponseEntity<ResponseDto<Void>> purge(
            @PathVariable(name = "mailNo") Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.purge(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "완전 삭제 성공", null));
    }

    @PatchMapping("/{mailNo}/prior")
    public ResponseEntity<ResponseDto<Void>> setPrior(
            @PathVariable(name = "mailNo") Long mailNo,
            @RequestParam("prior") boolean prior,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.setPrior(mailNo, empId, prior);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "중요 메일 설정 성공", null));
    }

    @PatchMapping("/{mailNo}/prior/toggle")
    public ResponseEntity<ResponseDto<Void>> togglePrior(
            @PathVariable(name = "mailNo") Long mailNo,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.togglePrior(mailNo, empId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "중요 메일 토글 성공", null));
    }

    // ===== drafts =====
    @GetMapping("/drafts")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> drafts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        Page<ResMailListDto> res = mailService.getDrafts(empId, keyword, pageable);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "임시저장 목록 조회 성공", res));
    }

    @GetMapping("/drafts/{mailId}")
    public ResponseEntity<ResponseDto<ResMailDraftDetailDto>> draftDetail(
            @PathVariable(name = "mailId") String mailId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailDraftDetailDto res = mailService.getDraftDetail(mailId, empId);

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
        boolean isCreate = (req.mailId() == null || req.mailId().isBlank());

        ResMailDraftSavedDto res = mailService.saveDraft(empId, req);

        HttpStatus status = isCreate ? HttpStatus.CREATED : HttpStatus.OK;
        String msg = isCreate ? "임시저장 생성 성공" : "임시저장 수정 성공";

        return ResponseEntity
                .status(status)
                .body(new ResponseDto<>(status, msg, res));
    }

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

    @PostMapping("/drafts/{mailId}/send")
    public ResponseEntity<ResponseDto<ResMailSendDto>> sendDraft(
            @PathVariable(name = "mailId") String mailId,
            @RequestBody(required = false) ReqMailDraftSendDto req,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResMailSendDto res = mailService.sendDraft(mailId, empId, req);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "임시저장 발송 성공", res));
    }

    @GetMapping("/self")
    public ResponseEntity<ResponseDto<Page<ResMailListDto>>> selfMailbox(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "priorOnly", required = false) Boolean priorOnly,
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();

        Page<ResMailListDto> res = Boolean.TRUE.equals(priorOnly)
                ? mailService.getPriorSelfMailbox(empId, keyword, from, to, pageable)
                : mailService.getSelfMailbox(empId, keyword, from, to, pageable);

        String msg = Boolean.TRUE.equals(priorOnly)
                ? "중요 내게쓴메일함 조회 성공"
                : "내게쓴메일함 조회 성공";

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, msg, res));
    }

    @DeleteMapping("/drafts/{mailId}/attachments/{attachmentId}")
    public ResponseEntity<ResponseDto<Void>> deleteDraftAttachment(
            @PathVariable String mailId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        mailService.deleteDraftAttachment(mailId, attachmentId, empId);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "임시저장 첨부 삭제 성공", null));
    }
}