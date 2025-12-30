package com.multi.mlpenterpriseapprovalsystem.mail.service;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.*;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;
import com.multi.mlpenterpriseapprovalsystem.mail.repository.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

import java.time.*;
import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : MailServiceImpl
 * @since : 2025-12-30 화요일
 */

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService{
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

        // 발신자 상태 row (보낸 메일함/삭제 상태 관리용)
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

    // 보낸 메일함
    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getSent(String senderEmpId, Pageable pageable) {
        return mailRepository.findSentMails(senderEmpId, pageable)
                .map(m -> new ResMailListDto(
                        m.getMailId(),
                        m.getTitle(),
                        m.getSender().getEmpId(),
                        m.getSender().getEmpName(),
                        MailRole.SENDER,
                        true,          // 발신자는 읽음 true 처리
                        false,
                        null,          // sent 목록에서 deletedAt은 “발신자 userState”를 조회해야 정확
                        m.getCreatedAt()
                ));
    }

    // 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ResMailDetailDto getDetail(String mailId, String viewerEmpId) {
        Mail mail = mailRepository.findDetailByMailId(mailId)
                .orElseThrow(() -> new NoSuchElementException("메일 없음: " + mailId));

        MailUserState mus = mailUserStateRepository.findByMail_MailIdAndUser_EmpId(mailId, viewerEmpId)
                .orElseThrow(() -> new NoSuchElementException("메일 상태 없음. mailId=" + mailId + ", viewer=" + viewerEmpId));

        return new ResMailDetailDto(
                mail.getMailId(),
                mail.getTitle(),
                mail.getCntt(),
                mail.getSender().getEmpId(),
                mail.getSender().getEmpName(), // Employee 필드명 맞춰서 수정
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

    // Mapper
    private ResMailListDto toListDto(MailUserState mus) {
        Mail m = mus.getMail();
        return new ResMailListDto(
                m.getMailId(),
                m.getTitle(),
                m.getSender().getEmpId(),
                m.getSender().getEmpName(), // Employee 필드명 맞춰서 수정
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
