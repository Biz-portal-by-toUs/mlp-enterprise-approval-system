package com.multi.mlpenterpriseapprovalsystem.mail.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.*;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;
import com.multi.mlpenterpriseapprovalsystem.mail.repository.MailRepository;
import com.multi.mlpenterpriseapprovalsystem.mail.repository.MailUserStateRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.*;
import com.multi.mlpenterpriseapprovalsystem.notification.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

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
                .orElseThrow(() -> new NoSuchElementException("발신자(empId) 없음: " + senderEmpId));

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
                        .orElseThrow(() -> new NoSuchElementException("수신자(empId) 없음: " + recvEmpId));
                mailUserStateRepository.save(MailUserState.create(saved, recv, MailRole.RECIPIENT));
                noti.sendNotification(recv.getEmpId(), NotificationType.MAIL, "[메일]", saved.getTitle(), "/mail/"+mail.getMailNo());
            }
        }

        return new ResMailSendDto(saved.getMailId(), saved.getMailNo(), saved.getCreatedAt());
    }

    // 받은 메일함
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInbox(CustomUser user, Pageable pageable) {

        Employee emp = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        return mailUserStateRepository.findInbox(emp.getEmpId(), MailRole.RECIPIENT, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInboxByRoles(String userEmpId, List<MailRole> roles, Pageable pageable) {
        return mailUserStateRepository.findInboxByRoles(userEmpId, roles, pageable)
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
                .orElseThrow(() -> new AccessDeniedException("해당 메일에 대한 접근 권한이 없습니다."));

        Mail mail = mus.getMail();

        String cnttJson = mail.getCntt();
        String cnttHtml = null;

        String receivers = null;
        if (mus.getRole() == MailRole.SENDER) {
            List<String> receiverDisplays = mailUserStateRepository.findRecipientDisplayByMailId(mailId);

            // 혹시 데이터에 중복이 있으면 방어적으로 중복 제거
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
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new AccessDeniedException("메일 상태 없음 또는 권한 없음."));
        mus.markRead();
    }

    // 휴지통 이동
    @Override
    @Transactional
    public void moveToTrash(String mailId, String userEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new AccessDeniedException("메일 상태 없음 또는 권한 없음."));
        mus.moveToTrash(LocalDateTime.now());
    }

    // 휴지통 복원
    @Override
    @Transactional
    public void restoreFromTrash(String mailId, String userEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new AccessDeniedException("메일 상태 없음 또는 권한 없음."));
        mus.restore();
    }

    // 완전 삭제
    @Override
    @Transactional
    public void purge(String mailId, String userEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new AccessDeniedException("메일 상태 없음 또는 권한 없음."));

        if (mus.getDeletedAt() == null) {
            throw new IllegalStateException("완전 삭제는 휴지통을 거친 메일만 가능합니다. mailId=" + mailId);
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
                .orElseThrow(() -> new NoSuchElementException("발신자(empId) 없음: " + senderEmpId));

        String title = (req.title() == null) ? "" : req.title().trim();
        String cnttJson = (req.cnttJson() == null) ? "{}" : req.cnttJson();

        Mail mail;
        if (req.mailId() == null || req.mailId().isBlank()) {
            String mailId = generateMailId(senderEmpId);

            // 신규 임시저장 생성
            mail = Mail.createDraft(mailId, title, cnttJson, sender);

        } else {
            // 기존 초안 업데이트
            mail = mailRepository.findDraftDetail(req.mailId(), senderEmpId)
                    .orElseThrow(() -> new AccessDeniedException("초안이 없거나 권한이 없습니다. mailId=" + req.mailId()));

            // 방어: 혹시 이미 발송된 메일이면 막기
            if (!mail.isDraft()) {
                throw new IllegalStateException("이미 발송된 메일은 임시저장 수정할 수 없습니다. mailId=" + req.mailId());
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
                .orElseThrow(() -> new AccessDeniedException("초안이 없거나 권한이 없습니다. mailId=" + mailId));

        // draft는 mus가 없으니 role은 SENDER로 고정, receivers도 비움
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
            throw new AccessDeniedException("삭제할 초안이 없거나 권한이 없습니다. mailId=" + mailId);
        }
    }

    // 임시저장 -> 발송
    @Override
    @Transactional
    public ResMailSendDto sendDraft(String mailId, String senderEmpId, ReqMailDraftSendDto req) {
        Mail mail = mailRepository.findDraftDetail(mailId, senderEmpId)
                .orElseThrow(() -> new AccessDeniedException("초안이 없거나 권한이 없습니다. mailId=" + mailId));

        if (!mail.isDraft()) {
            throw new IllegalStateException("이미 발송된 메일입니다. mailId=" + mailId);
        }

        Employee sender = employeeRepository.findByEmpId(senderEmpId)
                .orElseThrow(() -> new NoSuchElementException("발신자(empId) 없음: " + senderEmpId));

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
                        .orElseThrow(() -> new NoSuchElementException("수신자(empId) 없음: " + recvEmpId));

                // 중복 방지
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

    // mailId 생성 규칙
    private String generateMailId(String senderEmpId) {
        long epochSec = Instant.now().getEpochSecond();
        return "MAIL_" + epochSec + "_" + senderEmpId;
    }

    @Override
    @Transactional
    public void setPrior(String mailId, String userEmpId, boolean prior) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new AccessDeniedException("메일 상태 없음 또는 권한 없음."));
        mus.setPrior(prior);
    }

    @Override
    @Transactional
    public void togglePrior(String mailId, String userEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new AccessDeniedException("메일 상태 없음 또는 권한 없음."));
        mus.togglePrior();
    }
}