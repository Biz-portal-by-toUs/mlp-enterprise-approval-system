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

        // 수신자 정규화
        Set<String> unique = new LinkedHashSet<>();
        if (req.receiverEmpIds() != null) {
            for (String id : req.receiverEmpIds()) {
                if (id != null && !id.isBlank()) unique.add(id.trim());
            }
        }

        boolean isSelfMail = (unique.size() == 1 && unique.contains(senderEmpId));

        if (isSelfMail) {
            // 내게쓴메일: RECIPIENT row만 1개
            mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.RECIPIENT));
        } else {
            // 일반메일: SENDER row 1개 + RECIPIENT rows
            mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.SENDER));

            for (String recvEmpId : unique) {
                Employee recv = employeeRepository.findByEmpId(recvEmpId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MAIL_RECEIVER_NOT_FOUND));

                mailUserStateRepository.save(MailUserState.create(saved, recv, MailRole.RECIPIENT));

                noti.sendNotification(
                        recv.getEmpId(),
                        NotificationType.MAIL,
                        "[메일]",
                        saved.getTitle(),
                        "/mail/" + saved.getMailNo()
                );
            }
        }

        return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInbox(String userEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findMailbox(userEmpId, MailRole.RECIPIENT, keyword, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getSent(String senderEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findMailbox(senderEmpId, MailRole.SENDER, keyword, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getTrash(String userEmpId, Pageable pageable) {
        return mailUserStateRepository.findTrash(userEmpId, pageable)
                .map(this::toListDto);
    }

    // detail / state : mailNo 기준
    @Override
    @Transactional(readOnly = true)
    public ResMailDetailDto getDetail(Long mailNo, String viewerEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailNoAndUser_EmpId(mailNo, viewerEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_ACCESS_DENIED));

        return buildDetailDtoFromState(mus);
    }

    // 읽음 표시
    @Override
    @Transactional
    public void markAsRead(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.markRead();
    }

    // 삭제(휴지통)
    @Override
    @Transactional
    public void moveToTrash(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.moveToTrash(LocalDateTime.now());
    }

    // 메일 복구
    @Override
    @Transactional
    public void restoreFromTrash(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.restore();
    }

    // 메일 완전 삭제
    @Override
    @Transactional
    public void purge(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);

        if (mus.getDeletedAt() == null) {
            throw new CustomException(ErrorCode.MAIL_PURGE_ONLY_AFTER_TRASH);
        }
        mailUserStateRepository.delete(mus);
    }

    // 즐겨찾기 설정
    @Override
    @Transactional
    public void setPrior(Long mailNo, String userEmpId, boolean prior) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.setPrior(prior);
    }

    // 즐겨찾기 변경
    @Override
    @Transactional
    public void togglePrior(Long mailNo, String userEmpId) {
        MailUserState mus = mustFindState(mailNo, userEmpId);
        mus.togglePrior();
    }

    // DTO builders
    private ResMailListDto toListDto(MailUserState mus) {
        Mail m = mus.getMail();

        String receivers = null;
        if (mus.getRole() == MailRole.SENDER) {
            // mailNo 기준으로 통일
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
            // mailNo 기준
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

    // 임시 저장 (mailId 기반 유지)
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

        // DTO: (mailNo, mailId, savedAt)
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
        Mail draft = mailRepository.findDraftDetail(mailId, senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND));

        if (!draft.isDraft()) throw new CustomException(ErrorCode.MAIL_ALREADY_SENT);

        Employee sender = employeeRepository.findByEmpId(senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_SENDER_NOT_FOUND));

        // 수신자 정규화
        Set<String> unique = new LinkedHashSet<>();
        if (req.receiverEmpIds() != null) {
            for (String id : req.receiverEmpIds()) {
                if (id != null && !id.isBlank()) unique.add(id.trim());
            }
        }

        boolean wantSelf = unique.contains(senderEmpId);

        // others = 나를 제외한 수신자
        Set<String> others = new LinkedHashSet<>(unique);
        others.remove(senderEmpId);

        // case 1) 내게쓰기만
        if (wantSelf && others.isEmpty()) {
            draft.clearDraft();
            Mail savedSelf = mailRepository.save(draft);

            // sender-row 없이 recipient-row만
            mailUserStateRepository.findByMail_MailNoAndUser_EmpId(savedSelf.getMailNo(), senderEmpId)
                    .orElseGet(() -> mailUserStateRepository.save(
                            MailUserState.create(savedSelf, sender, MailRole.RECIPIENT)
                    ));

            return new ResMailSendDto(savedSelf.getMailNo(), savedSelf.getMailId(), savedSelf.getSavedAt());
        }

        // case 2) 일반메일만(내가 수신자에 없음)
        if (!wantSelf) {
            draft.clearDraft();
            Mail saved = mailRepository.save(draft);

            // sender-row
            mailUserStateRepository.findByMail_MailNoAndUser_EmpId(saved.getMailNo(), senderEmpId)
                    .orElseGet(() -> mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.SENDER)));

            // recipients
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

            return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getSavedAt());
        }

        // ========== case 3) 내게쓰기 + 다른사람(복제 발송) ==========
        // 3-1) 원본 draft를 self-mail로 확정
        draft.clearDraft();
        Mail savedSelf = mailRepository.save(draft);

        mailUserStateRepository.findByMail_MailNoAndUser_EmpId(savedSelf.getMailNo(), senderEmpId)
                .orElseGet(() -> mailUserStateRepository.save(
                        MailUserState.create(savedSelf, sender, MailRole.RECIPIENT)
                ));

        // 3-2) 내용 복제해서 "일반메일"을 새로 생성
        String newMailId = generateMailId(senderEmpId);

        Mail clone = Mail.create(newMailId, savedSelf.getTitle(), savedSelf.getCntt(), sender);
        Mail savedNormal = mailRepository.save(clone);

        // sender-row
        mailUserStateRepository.save(MailUserState.create(savedNormal, sender, MailRole.SENDER));

        // recipients(others만)
        for (String recvEmpId : others) {
            Employee recv = employeeRepository.findByEmpId(recvEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MAIL_RECEIVER_NOT_FOUND));

            mailUserStateRepository.save(MailUserState.create(savedNormal, recv, MailRole.RECIPIENT));

            noti.sendNotification(
                    recv.getEmpId(),
                    NotificationType.MAIL,
                    "[메일]",
                    savedNormal.getTitle(),
                    "/mail/" + savedNormal.getMailNo()
            );
        }

        // 응답은 일반메일을 기준
        return new ResMailSendDto(savedNormal.getMailNo(), savedNormal.getMailId(), savedNormal.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getSelfMailbox(String userEmpId, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findSelfMailbox(userEmpId, keyword, pageable)
                .map(this::toListDto);
    }

    // helpers
    private String generateMailId(String senderEmpId) {
        return "MAIL_" + Instant.now().toEpochMilli() + "_" + senderEmpId;
    }

    private MailUserState mustFindState(Long mailNo, String userEmpId) {
        return mailUserStateRepository.findByMail_MailNoAndUser_EmpId(mailNo, userEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_STATE_NOT_FOUND));
    }
}