package com.multi.mlpenterpriseapprovalsystem.mail.service;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.Mail;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.MailUserState;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.ReqMailSendDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailDetailDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailListDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailSendDto;
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
    public Page<ResMailListDto> getInbox(String userEmpId, MailRole role, String q, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        return mailUserStateRepository.findInbox(userEmpId, role, keyword, pageable)
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