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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

/**
 * MailService 구현체
 */
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final MailRepository mailRepository;
    private final MailUserStateRepository mailUserStateRepository;
    private final EmployeeRepository employeeRepository;

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
            }
        }

        return new ResMailSendDto(saved.getMailId(), saved.getMailNo(), saved.getCreatedAt());
    }

    // 받은 메일함
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInbox(String userEmpId, MailRole role, Pageable pageable) {
        return mailUserStateRepository.findInbox(userEmpId, role, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInboxByRoles(String userEmpId, List<MailRole> roles, Pageable pageable) {
        return mailUserStateRepository.findInboxByRoles(userEmpId, roles, pageable)
                .map(this::toListDto);
    }

    // ✅ 보낸 메일함 (MailUserState 기반 + 수신인 receivers 포함)
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getSent(String senderEmpId, Pageable pageable) {

        // senderEmpId는 "보낸 사람(로그인 사용자)"의 empId
        return mailUserStateRepository.findSent(senderEmpId, MailRole.SENDER, pageable)
                .map(mus -> {
                    Mail m = mus.getMail();

                    // 수신인 이름들
                    List<String> names = mailUserStateRepository.findRecipientNamesByMailId(m.getMailId());
                    String receivers = (names == null || names.isEmpty()) ? "-" : String.join(", ", names);

                    return new ResMailListDto(
                            m.getMailId(),
                            m.getTitle(),

                            m.getSender().getEmpId(),
                            m.getSender().getEmpName(),

                            receivers,                 // ✅ sent 화면용
                            MailRole.SENDER,

                            true,                      // 발신자는 읽음 true 처리
                            Boolean.TRUE.equals(mus.getIsPrior()),
                            mus.getDeletedAt(),         // sent에서도 삭제상태 보려면 여기 사용 가능
                            m.getCreatedAt()
                    );
                });
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
        Mail mail = mailRepository.findDetailByMailId(mailId)
                .orElseThrow(() -> new NoSuchElementException("메일 없음: " + mailId));

        MailUserState mus = mailUserStateRepository.findState(mailId, viewerEmpId)
                .orElseThrow(() -> new NoSuchElementException("메일 상태 없음. mailId=" + mailId + ", viewer=" + viewerEmpId));

        return new ResMailDetailDto(
                mail.getMailId(),
                mail.getTitle(),
                mail.getCntt(),
                mail.getSender().getEmpId(),
                mail.getSender().getEmpName(),
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
                .orElseThrow(() -> new NoSuchElementException("메일 상태 없음. mailId=" + mailId + ", user=" + userEmpId));
        mus.markRead();
    }

    // 휴지통 이동
    @Override
    @Transactional
    public void moveToTrash(String mailId, String userEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new NoSuchElementException("메일 상태 없음. mailId=" + mailId + ", user=" + userEmpId));
        mus.moveToTrash(LocalDateTime.now());
    }

    // 휴지통 복원
    @Override
    @Transactional
    public void restoreFromTrash(String mailId, String userEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new NoSuchElementException("메일 상태 없음. mailId=" + mailId + ", user=" + userEmpId));
        mus.restore();
    }

    // 완전 삭제
    @Override
    @Transactional
    public void purge(String mailId, String userEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, userEmpId)
                .orElseThrow(() -> new NoSuchElementException("메일 상태 없음. mailId=" + mailId + ", user=" + userEmpId));

        if (mus.getDeletedAt() == null) {
            throw new IllegalStateException("완전 삭제는 휴지통을 거친 메일만 가능합니다. mailId=" + mailId);
        }

        mailUserStateRepository.delete(mus);
    }

    // Mapper (inbox/trash 공용)
    private ResMailListDto toListDto(MailUserState mus) {
        Mail m = mus.getMail();
        return new ResMailListDto(
                m.getMailId(),
                m.getTitle(),

                m.getSender().getEmpId(),
                m.getSender().getEmpName(),

                null, // ✅ inbox/trash에서는 사용 안 하면 null로
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
}