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

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final MailRepository mailRepository;
    private final MailUserStateRepository mailUserStateRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService noti;

    // =========================
    // ===== send / list =====
    // =========================

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

                // 알림 링크는 mailNo
                noti.sendNotification(
                        recv.getEmpId(),
                        NotificationType.MAIL,
                        "[메일]",
                        saved.getTitle(),
                        "/mail/" + saved.getMailNo()
                );
            }
        }

        // ✅ DTO: (mailNo, mailId, createdAt)
        return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInbox(String userEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findInbox(userEmpId, MailRole.RECIPIENT, keyword, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getSent(String senderEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findSent(senderEmpId, MailRole.SENDER, keyword, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getTrash(String userEmpId, Pageable pageable) {
        return mailUserStateRepository.findTrash(userEmpId, pageable)
                .map(this::toListDto);
    }

    // =========================
    // ✅ detail / state : mailNo 기준
    // =========================

    @Override
    @Transactional(readOnly = true)
    public ResMailDetailDto getDetail(Long mailNo, String viewerEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailNoAndUser_EmpId(mailNo, viewerEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_ACCESS_DENIED));

        return buildDetailDtoFromState(mus);
    }

    @Override
    @Transactional
    public void markAsRead(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.markRead();
    }

    @Override
    @Transactional
    public void moveToTrash(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.moveToTrash(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void restoreFromTrash(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.restore();
    }

    @Override
    @Transactional
    public void purge(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);

        if (mus.getDeletedAt() == null) {
            throw new CustomException(ErrorCode.MAIL_PURGE_ONLY_AFTER_TRASH);
        }
        mailUserStateRepository.delete(mus);
    }

    @Override
    @Transactional
    public void setPrior(Long mailNo, String userEmpId, boolean prior) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.setPrior(prior);
    }

    @Override
    @Transactional
    public void togglePrior(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.togglePrior();
    }

    // =========================
    // ===== DTO builders =====
    // =========================

    private ResMailListDto toListDto(MailUserState mus) {
        Mail m = mus.getMail();

        String receivers = null;
        if (mus.getRole() == MailRole.SENDER) {
            // ✅ mailNo 기준으로 통일
            List<String> names = mailUserStateRepository.findRecipientNamesByMailNo(m.getMailNo());
            receivers = (names == null || names.isEmpty()) ? "-" : String.join(", ", names);
        }

        return new ResMailListDto(
                m.getMailNo(),
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

    private ResMailDetailDto buildDetailDtoFromState(MailUserState mus) {
        Mail mail = mus.getMail();

        String receivers = null;
        if (mus.getRole() == MailRole.SENDER) {
            // ✅ mailNo 기준
            List<String> receiverDisplays = mailUserStateRepository.findRecipientDisplayByMailNo(mail.getMailNo());
            receivers = receiverDisplays.stream()
                    .distinct()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }

        return new ResMailDetailDto(
                mail.getMailNo(),
                mail.getMailId(),
                mail.getTitle(),
                mail.getCntt(),
                null,
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

    // =========================
    // ===== drafts (mailId 기반 유지) =====
    // =========================

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

        // ✅ DTO: (mailNo, mailId, savedAt)
        return new ResMailDraftSavedDto(saved.getMailNo(), saved.getMailId(), saved.getSavedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getDrafts(String senderEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();

        return mailRepository.findDrafts(senderEmpId, keyword, pageable)
                .map(m -> new ResMailListDto(
                        m.getMailNo(),
                        m.getMailId(),
                        m.getTitle(),
                        m.getSender().getEmpId(),
                        m.getSender().getEmpName(),
                        "-",
                        MailRole.SENDER, // 화면 재사용용
                        false,
                        false,
                        null,
                        m.getCreatedAt()
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public ResMailDetailDto getDraftDetail(String mailId, String senderEmpId) {
        Mail mail = mailRepository.findDraftDetail(mailId, senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND));

        return new ResMailDetailDto(
                mail.getMailNo(),
                mail.getMailId(),
                mail.getTitle(),
                mail.getCntt(),
                null,
                mail.getSender().getEmpId(),
                mail.getSender().getEmpName(),
                "",
                MailRole.SENDER,
                false,
                false,
                null,
                mail.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public void deleteDraft(String mailId, String senderEmpId) {
        int deleted = mailRepository.deleteDraft(mailId, senderEmpId);
        if (deleted == 0) throw new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND);
    }

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

        // sender state row 생성(중복 방지) - 여기만 mailId 기반 유지해도 OK
        mailUserStateRepository.findByMail_MailNoAndUser_EmpId(saved.getMailNo(), senderEmpId)
                .orElseGet(() -> mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.SENDER)));

        if (receiverEmpIds != null && !receiverEmpIds.isEmpty()) {
            Set<String> unique = new LinkedHashSet<>();
            for (String id : receiverEmpIds) {
                if (id != null && !id.isBlank()) unique.add(id.trim());
            }

            for (String recvEmpId : unique) {
                Employee recv = employeeRepository.findByEmpId(recvEmpId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MAIL_RECEIVER_NOT_FOUND));

                mailUserStateRepository.findByMail_MailNoAndUser_EmpId(saved.getMailNo(), recvEmpId)
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

        // ✅ DTO: (mailNo, mailId, createdAt) / savedAt은 sent면 null일 수도
        return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getSavedAt());
    }

    // =========================
    // ===== helpers =====
    // =========================

    private String generateMailId(String senderEmpId) {
        long epochSec = Instant.now().getEpochSecond();
        return "MAIL_" + epochSec + "_" + senderEmpId;
    }

    private MailUserState mustFindState(Long mailNo, String userEmpId) {
        return mailUserStateRepository.findByMail_MailNoAndUser_EmpId(mailNo, userEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_STATE_NOT_FOUND));
    }
}