package com.multi.mlpenterpriseapprovalsystem.mail.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.Mail;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.MailUserState;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;
import com.multi.mlpenterpriseapprovalsystem.mail.repository.MailRepository;
import com.multi.mlpenterpriseapprovalsystem.mail.repository.MailUserStateRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 메일 서비스(실행부)
 *
 * @author : 정종원
 * @filename : MailServiceImpl
 * @since : 2025-12-30 화요일
 */

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final MailRepository mailRepository;
    private final MailUserStateRepository mailUserStateRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService noti;

    // 메일 전송
    @Override
    @Transactional
    public ResMailSendDto sendMail(String senderEmpId, ReqMailSendDto req) {
        Employee sender = employeeRepository.findByEmpId(senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_SENDER_NOT_FOUND));

        String mailId = generateMailId(senderEmpId);

        Mail mail = Mail.create(mailId, req.title(), req.cnttJson(), sender);
        Mail saved = mailRepository.save(mail);

        // 발신자 상태 row
        mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.SENDER));

        // 수신자 상태 rows
        if (req.receiverEmpIds() != null && !req.receiverEmpIds().isEmpty()) {
            Set<String> unique = new LinkedHashSet<>(req.receiverEmpIds());
            for (String recvEmpId : unique) {
                Employee recv = employeeRepository.findByEmpId(recvEmpId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MAIL_RECEIVER_NOT_FOUND));

                mailUserStateRepository.save(MailUserState.create(saved, recv, MailRole.RECIPIENT));

                noti.sendNotification(
                        recv.getEmpId(),
                        NotificationType.MAIL,
                        "[메일]",
                        saved.getTitle(),
                        "/mail/" + mail.getMailNo()
                );
            }
        }

        return new ResMailSendDto(saved.getMailId(), saved.getMailNo(), saved.getCreatedAt());
    }

    // 받은 메일함
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInbox(String userEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findInbox(userEmpId, MailRole.RECIPIENT, keyword, pageable)
                .map(this::toListDto);
    }

    // 보낸 메일함
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getSent(String senderEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findSent(senderEmpId, MailRole.SENDER, keyword, pageable)
                .map(this::toListDto);
    }

    // 휴지통 조회
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getTrash(String userEmpId, Pageable pageable) {
        return mailUserStateRepository.findTrash(userEmpId, pageable)
                .map(this::toListDto);
    }

    // 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ResMailDetailDto getDetail(String mailId, String viewerEmpId) {
        MailUserState mus = mailUserStateRepository.findState(mailId, viewerEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_ACCESS_DENIED));

        Mail mail = mus.getMail();

        String cnttJson = mail.getCntt();
        String cnttHtml = null;

        String receivers = null;
        if (mus.getRole() == MailRole.SENDER) {
            List<String> receiverDisplays = mailUserStateRepository.findRecipientDisplayByMailId(mailId);

            receivers = receiverDisplays.stream()
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }

        return new ResMailDetailDto(
                mail.getMailId(),
                mail.getTitle(),
                cnttJson,
                cnttHtml,
                mail.getSender().getEmpId(),
                mail.getSender().getEmpName(),
                receivers,
                mus.getRole(),
                Boolean.TRUE.equals(mus.getIsRead()),
                Boolean.TRUE.equals(mus.getIsPrior()),
                mus.getDeletedAt(),
                mail.getCreatedAt()
        );
    }

    // 읽음 처리
    @Override
    @Transactional
    public void markAsRead(String mailId, String userEmpId) {
        MailUserState mus = mustFindState(mailId, userEmpId);
        mus.markRead();
    }

    // 휴지통 이동
    @Override
    @Transactional
    public void moveToTrash(String mailId, String userEmpId) {
        MailUserState mus = mustFindState(mailId, userEmpId);
        mus.moveToTrash(LocalDateTime.now());
    }

    // 휴지통 복원
    @Override
    @Transactional
    public void restoreFromTrash(String mailId, String userEmpId) {
        MailUserState mus = mustFindState(mailId, userEmpId);
        mus.restore();
    }

    // 완전 삭제
    @Override
    @Transactional
    public void purge(String mailId, String userEmpId) {
        MailUserState mus = mustFindState(mailId, userEmpId);

        if (mus.getDeletedAt() == null) {
            throw new CustomException(ErrorCode.MAIL_PURGE_ONLY_AFTER_TRASH);
        }
        mailUserStateRepository.delete(mus);
    }

    // 목록 DTO 변환
    private ResMailListDto toListDto(MailUserState mus) {
        Mail m = mus.getMail();

        String receivers = null;
        if (mus.getRole() == MailRole.SENDER) {
            List<String> names = mailUserStateRepository.findRecipientNamesByMailId(m.getMailId());
            receivers = (names == null || names.isEmpty()) ? "-" : String.join(", ", names);
        }

        return new ResMailListDto(
                m.getMailId(),
                m.getTitle(),
                m.getSender().getEmpId(),
                m.getSender().getEmpName(),
                receivers,
                mus.getRole(),
                Boolean.TRUE.equals(mus.getIsRead()),
                Boolean.TRUE.equals(mus.getIsPrior()),
                mus.getDeletedAt(),
                m.getCreatedAt()
        );
    }

    // 임시저장 생성/수정
    @Override
    @Transactional
    public ResMailDraftSavedDto saveDraft(String senderEmpId, ReqMailDraftSaveDto req) {
        Employee sender = employeeRepository.findByEmpId(senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_SENDER_NOT_FOUND));

        String title = (req.title() == null) ? "" : req.title().trim();
        String cnttJson = (req.cnttJson() == null) ? "{}" : req.cnttJson();

        Mail mail;
        if (req.mailId() == null || req.mailId().isBlank()) {
            String mailId = generateMailId(senderEmpId);
            mail = Mail.createDraft(mailId, title, cnttJson, sender);

        } else {
            mail = mailRepository.findDraftDetail(req.mailId(), senderEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND));

            if (!mail.isDraft()) {
                throw new CustomException(ErrorCode.MAIL_ALREADY_SENT);
            }

            mail.updateDraft(title, cnttJson);
        }

        Mail saved = mailRepository.save(mail);
        return new ResMailDraftSavedDto(saved.getMailId(), saved.getMailNo(), saved.getSavedAt());
    }

    // 임시저장 목록
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getDrafts(String senderEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();

        return mailRepository.findDrafts(senderEmpId, keyword, pageable)
                .map(m -> new ResMailListDto(
                        m.getMailId(),
                        m.getTitle(),
                        m.getSender().getEmpId(),
                        m.getSender().getEmpName(),
                        "-",
                        MailRole.SENDER,     // 화면 재사용용
                        false,
                        false,
                        null,
                        m.getCreatedAt()
                ));
    }

    // 임시저장 상세
    @Override
    @Transactional(readOnly = true)
    public ResMailDetailDto getDraftDetail(String mailId, String senderEmpId) {
        Mail mail = mailRepository.findDraftDetail(mailId, senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND));

        return new ResMailDetailDto(
                mail.getMailId(),
                mail.getTitle(),
                mail.getCntt(),
                null, // cnttHtml 아직 없으면 null 유지
                mail.getSender().getEmpId(),
                mail.getSender().getEmpName(),
                "",              // receivers 없음
                MailRole.SENDER,
                false,
                false,
                null,
                mail.getCreatedAt()
        );
    }

    // 임시저장 삭제 (완전 삭제)
    @Override
    @Transactional
    public void deleteDraft(String mailId, String senderEmpId) {
        int deleted = mailRepository.deleteDraft(mailId, senderEmpId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND);
        }
    }

    // 임시저장 -> 발송
    @Override
    @Transactional
    public ResMailSendDto sendDraft(String mailId, String senderEmpId, ReqMailDraftSendDto req) {
        Mail mail = mailRepository.findDraftDetail(mailId, senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND));

        if (!mail.isDraft()) {
            throw new CustomException(ErrorCode.MAIL_ALREADY_SENT);
        }

        Employee sender = employeeRepository.findByEmpId(senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_SENDER_NOT_FOUND));

        List<String> receiverEmpIds = req.receiverEmpIds();

        // 초안 해제
        mail.clearDraft();
        Mail saved = mailRepository.save(mail);

        // sender state row 생성 (중복 방지)
        mailUserStateRepository.findByMail_MailIdAndUser_EmpId(saved.getMailId(), senderEmpId)
                .orElseGet(() -> mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.SENDER)));

        // recipients state rows 생성
        if (receiverEmpIds != null && !receiverEmpIds.isEmpty()) {
            Set<String> unique = new LinkedHashSet<>();
            for (String id : receiverEmpIds) {
                if (id != null && !id.isBlank()) unique.add(id.trim());
            }

            for (String recvEmpId : unique) {
                Employee recv = employeeRepository.findByEmpId(recvEmpId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MAIL_RECEIVER_NOT_FOUND));

                mailUserStateRepository.findByMail_MailIdAndUser_EmpId(saved.getMailId(), recvEmpId)
                        .orElseGet(() -> mailUserStateRepository.save(MailUserState.create(saved, recv, MailRole.RECIPIENT)));

                noti.sendNotification(
                        recv.getEmpId(),
                        NotificationType.MAIL,
                        "[메일]",
                        saved.getTitle(),
                        "/mail/" + saved.getMailNo()
                );
            }
        }

        return new ResMailSendDto(saved.getMailId(), saved.getMailNo(), saved.getSavedAt()); // sent면 savedAt=null
    }

    @Override
    @Transactional
    public void setPrior(String mailId, String userEmpId, boolean prior) {
        MailUserState mus = mustFindState(mailId, userEmpId);
        mus.setPrior(prior);
    }

    @Override
    @Transactional
    public void togglePrior(String mailId, String userEmpId) {
        MailUserState mus = mustFindState(mailId, userEmpId);
        mus.togglePrior();
    }

    // mailId 생성 규칙
    private String generateMailId(String senderEmpId) {
        long epochSec = Instant.now().getEpochSecond();
        return "MAIL_" + epochSec + "_" + senderEmpId;
    }

    // 상태 row 조회 공통 (없으면 예외)
    private MailUserState mustFindState(String mailId, String userEmpId) {
        return mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_STATE_NOT_FOUND));
    }
}
