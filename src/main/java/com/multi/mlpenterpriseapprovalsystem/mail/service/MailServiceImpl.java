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

import java.time.*;
import java.util.*;
import java.util.stream.*;

@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final MailRepository mailRepository;
    private final MailUserStateRepository mailUserStateRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService noti;

    // send / list
    @Override
    @Transactional
    public ResMailSendDto sendMail(String senderEmpId, ReqMailSendDto req) {
        Employee sender = employeeRepository.findByEmpId(senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_SENDER_NOT_FOUND));

        // 1수신자 정규화/검증 먼저
        Set<String> unique = new LinkedHashSet<>();
        if (req.receiverEmpIds() != null) {
            for (String id : req.receiverEmpIds()) {
                if (id != null && !id.isBlank()) unique.add(id.trim());
            }
        }

        // 발송은 수신자 필수
        if (unique.isEmpty()) {
            throw new CustomException(ErrorCode.MAIL_RECEIVER_REQUIRED);
        }

        boolean hasSelf = unique.contains(senderEmpId);

        // 내게쓰기(self) + others 혼합 금지
        if (hasSelf && unique.size() > 1) {
            throw new CustomException(ErrorCode.MAIL_SELF_ONLY_MODE);
        }

        // Mail 생성/저장
        String mailId = generateMailId(senderEmpId);
        Mail mail = Mail.create(mailId, req.title(), req.cnttJson(), sender);
        Mail saved = mailRepository.save(mail);

        // 3상태 저장
        if (hasSelf) {
            // self-only: RECIPIENT 1개만
            mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.RECIPIENT));
            return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getCreatedAt());
        }

        // normal: SENDER 1개 + RECIPIENT N개
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

        return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getCreatedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getInbox(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();

        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt   = (to == null) ? null : to.atTime(LocalTime.MAX);

        return mailUserStateRepository.findMailbox(userEmpId, MailRole.RECIPIENT, keyword, fromDt, toDt, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getSent(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();

        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt   = (to == null) ? null : to.atTime(LocalTime.MAX);

        return mailUserStateRepository.findMailbox(userEmpId, MailRole.SENDER, keyword, fromDt, toDt, pageable)
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
        String raw = mus.getMail().getCntt();
        boolean hasLockedQuote = raw != null && raw.contains("\"lockedQuote\"");
        System.out.println("[detail] mailNo=" + mailNo + " hasLockedQuote=" + hasLockedQuote
                + " len=" + (raw == null ? 0 : raw.length()));
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

        String receiversText = null;
        List<String> receiverEmpIds = null;
        List<String> receiverNames = null;

        if (mus.getRole() == MailRole.SENDER) {
            receiverEmpIds = mailUserStateRepository.findRecipientEmpIdsByMailNo(mail.getMailNo());
            receiverNames = mapEmpIdsToNames(receiverEmpIds);
            receiversText = buildReceiversText(receiverEmpIds);
        }

        return new ResMailDetailDto(
                mail.getMailNo(),
                mail.getMailId(),
                mail.getTitle(),
                mail.getCntt(),
                null,
                mail.getSender().getEmpId(),
                mail.getSender().getEmpName(),
                receiversText,
                receiverEmpIds,
                receiverNames,
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

        // 제목/내용 기본값 처리
        String title = (req.title() == null) ? "" : req.title().trim();
        String cnttJson = (req.cnttJson() == null) ? "{}" : req.cnttJson();

        // 수신자 정규화
        List<String> receiverEmpIds = normalizeEmpIds(req.receiverEmpIds());

        // 정책: 내게쓰기(self-only) 혼합 금지
        boolean hasSelf = receiverEmpIds.contains(senderEmpId);
        if (hasSelf && receiverEmpIds.size() > 1) {
            throw new CustomException(ErrorCode.MAIL_SELF_ONLY_MODE);
        }

        // TEXT로 저장
        String draftReceivers = joinEmpIds(receiverEmpIds); // 비어있으면 null 처리됨(아래 joinEmpIds 기준)

        // 신규/수정 분기
        Mail mail;
        boolean isCreate = (req.mailId() == null || req.mailId().isBlank());

        if (isCreate) {
            String mailId = generateMailId(senderEmpId);
            mail = Mail.createDraft(mailId, title, cnttJson, sender);

            // 신규 draft에도 수신자 저장
            mail.updateDraft(title, cnttJson, draftReceivers);
        } else {
            mail = mailRepository.findDraftDetail(req.mailId(), senderEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND));

            if (!mail.isDraft()) {
                throw new CustomException(ErrorCode.MAIL_ALREADY_SENT);
            }

            // draft 업데이트 시 수신자까지 저장
            mail.updateDraft(title, cnttJson, draftReceivers);
        }

        // 5저장
        Mail saved = mailRepository.save(mail);

        return new ResMailDraftSavedDto(
                saved.getMailNo(),
                saved.getMailId(),
                saved.getSavedAt(),
                receiverEmpIds
        );
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
    public ResMailDraftDetailDto getDraftDetail(String mailId, String senderEmpId) {
        Mail mail = mailRepository.findDraftDetail(mailId, senderEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_DRAFT_NOT_FOUND));

        // empId 목록 (draftReceivers TEXT -> List)
        List<String> receiverEmpIds = splitEmpIds(mail.getDraftReceivers());

        // empName 목록 (IN 한방)
        List<String> receiverNames = mapEmpIdsToNames(receiverEmpIds);

        // UI 표시용 문자열
        String receiversText = buildReceiversText(receiverEmpIds);

        return new ResMailDraftDetailDto(
                mail.getMailNo(),
                mail.getMailId(),
                mail.getTitle(),
                mail.getCntt(),
                mail.getSavedAt(),
                receiverEmpIds,
                receiverNames,
                receiversText
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

        // 수신자 결정
        List<String> receiverList = normalizeEmpIds(req == null ? null : req.receiverEmpIds());
        if (receiverList.isEmpty()) {
            receiverList = splitEmpIds(draft.getDraftReceivers());
        }

        if (receiverList.isEmpty()) throw new CustomException(ErrorCode.MAIL_RECEIVER_REQUIRED);

        Set<String> unique = new LinkedHashSet<>(receiverList);

        boolean hasSelf = unique.contains(senderEmpId);
        if (hasSelf && unique.size() > 1) throw new CustomException(ErrorCode.MAIL_SELF_ONLY_MODE);

        // 제목/본문 결정
        String finalTitle = (req != null) ? req.title() : null;
        String finalCnttJson = (req != null) ? req.cnttJson() : null;

        // fallback
        if (finalTitle == null) finalTitle = draft.getTitle();
        if (finalCnttJson == null) finalCnttJson = draft.getCntt();

        finalTitle = (finalTitle == null) ? "" : finalTitle.trim();
        finalCnttJson = (finalCnttJson == null) ? "" : finalCnttJson.trim();

        // 빈 doc은 정책대로 막기
        if (finalTitle.isBlank()) throw new CustomException(ErrorCode.MAIL_TITLE_REQUIRED);
        if (finalCnttJson.isBlank() || "{}".equals(finalCnttJson)) throw new CustomException(ErrorCode.MAIL_CONTENT_REQUIRED);

        // draft 엔티티에 최종값 "확정"으로 세팅
        // draftReceivers는 receiverList를 기준으로 다시 저장하는 게 안전
        String finalDraftReceivers = joinEmpIds(new ArrayList<>(unique));
        draft.updateDraft(finalTitle, finalCnttJson, finalDraftReceivers);

        // draft -> sent 전환
        draft.applySendContent(finalTitle, finalCnttJson);
        draft.clearDraft();

        Mail saved = mailRepository.save(draft);

        // 5) MailUserState 정리
        if (hasSelf) {
            Optional<MailUserState> opt = mailUserStateRepository
                    .findByMail_MailNoAndUser_EmpId(saved.getMailNo(), senderEmpId);

            if (opt.isEmpty()) {
                mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.RECIPIENT));
            } else {
                MailUserState mus = opt.get();
                if (mus.getRole() == MailRole.SENDER) {
                    mailUserStateRepository.delete(mus);
                    mailUserStateRepository.save(MailUserState.create(saved, sender, MailRole.RECIPIENT));
                }
            }

            return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getCreatedAt());
        }

        // sender
        mailUserStateRepository.findByMail_MailNoAndUser_EmpId(saved.getMailNo(), senderEmpId)
                .orElseGet(() -> mailUserStateRepository.save(
                        MailUserState.create(saved, sender, MailRole.SENDER)
                ));

        // recipients + noti
        for (String recvEmpId : unique) {
            Employee recv = employeeRepository.findByEmpId(recvEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MAIL_RECEIVER_NOT_FOUND));

            Optional<MailUserState> opt = mailUserStateRepository
                    .findByMail_MailNoAndUser_EmpId(saved.getMailNo(), recvEmpId);

            if (opt.isEmpty()) {
                mailUserStateRepository.save(MailUserState.create(saved, recv, MailRole.RECIPIENT));
            } else {
                MailUserState mus = opt.get();
                if (mus.getRole() != MailRole.RECIPIENT) {
                    mailUserStateRepository.delete(mus);
                    mailUserStateRepository.save(MailUserState.create(saved, recv, MailRole.RECIPIENT));
                }
            }

            noti.sendNotification(
                    recv.getEmpId(),
                    NotificationType.MAIL,
                    "[메일]",
                    saved.getTitle(),
                    "/mail/" + saved.getMailNo()
            );
        }

        return new ResMailSendDto(saved.getMailNo(), saved.getMailId(), saved.getCreatedAt());
    }

    public Page<ResMailListDto> getSelfMailbox(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();

        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt   = (to == null) ? null : to.atTime(LocalTime.MAX);

        return mailUserStateRepository.findSelfMailbox(userEmpId, keyword, fromDt, toDt, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ResMailReplyPayloadDto getReplyPayload(Long mailNo, String viewerEmpId) {
        MailUserState mus = mailUserStateRepository.findByMail_MailNoAndUser_EmpId(mailNo, viewerEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_ACCESS_DENIED));

        // 수신자가 아닐 때 답신 불가 정책
        if (mus.getRole() != MailRole.RECIPIENT) {
            throw new CustomException(ErrorCode.MAIL_REPLY_ONLY_RECIPIENT);
        }

        Mail mail = mus.getMail();

        // 원문 제목
        String originTitle = (mail.getTitle() == null) ? "" : mail.getTitle().trim();

        // 답신 제목 생성 (RE: 중복 방지)
        String replyTitle = normalizeReplyTitle(originTitle);

        // 원 발신자
        Employee sender = mail.getSender();

        // 기본 수신자 = 원 발신자 1명
        List<ResMailReplyPayloadDto.ReceiverItem> receivers = List.of(
                new ResMailReplyPayloadDto.ReceiverItem(sender.getEmpId(), sender.getEmpName())
        );

        // 원문 JSON
        String quoteCnttJson = (mail.getCntt() == null) ? "" : mail.getCntt();

        String quoteHtml = null;

        // 중복 방지 키 (같은 원문메일에 대한 reply payload는 항상 동일한 키)
        String payloadKey = "reply:" + mailNo;

        return new ResMailReplyPayloadDto(
                payloadKey,
                mail.getMailNo(),
                replyTitle,
                receivers,
                quoteCnttJson,
                originTitle,
                quoteHtml,
                sender.getEmpId(),
                sender.getEmpName()
        );
    }

    @Override
    public long countUnreadInbox(String empId){
        return mailUserStateRepository.countUnreadInboxOnly(empId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getPriorInbox(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt   = (to == null) ? null : to.atTime(LocalTime.MAX);

        return mailUserStateRepository.findPriorMailbox(userEmpId, MailRole.RECIPIENT, keyword, fromDt, toDt, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getPriorSent(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt   = (to == null) ? null : to.atTime(LocalTime.MAX);

        return mailUserStateRepository.findPriorMailbox(userEmpId, MailRole.SENDER, keyword, fromDt, toDt, pageable)
                .map(this::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResMailListDto> getPriorSelfMailbox(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable) {
        String keyword = (q == null) ? null : q.trim();
        LocalDateTime fromDt = (from == null) ? null : from.atStartOfDay();
        LocalDateTime toDt   = (to == null) ? null : to.atTime(LocalTime.MAX);

        return mailUserStateRepository.findPriorSelfMailbox(userEmpId, keyword, fromDt, toDt, pageable)
                .map(this::toListDto);
    }

    // helpers
    private String generateMailId(String senderEmpId) {
        return "MAIL_" + Instant.now().toEpochMilli() + "_" + senderEmpId;
    }

    private String normalizeReplyTitle(String originTitle) {
        String t = (originTitle == null) ? "" : originTitle.trim();
        if (t.isBlank()) return "RE:";
        // "RE:" 또는 "Re:" 등 이미 붙어있으면 그대로
        if (t.regionMatches(true, 0, "RE:", 0, 3)) return t;
        return "RE: " + t;
    }

    private MailUserState mustFindState(Long mailNo, String userEmpId) {
        return mailUserStateRepository.findByMail_MailNoAndUser_EmpId(mailNo, userEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.MAIL_STATE_NOT_FOUND));
    }

    private List<String> normalizeEmpIds(List<String> raw) {
        if (raw == null) return List.of();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private String joinEmpIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return String.join(",", ids);
    }

    private List<String> splitEmpIds(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private List<String> mapEmpIdsToNames(List<String> empIds) {
        if (empIds == null || empIds.isEmpty()) return List.of();

        Map<String, String> nameMap = employeeRepository.findByEmpIdIn(empIds).stream()
                .collect(Collectors.toMap(Employee::getEmpId, Employee::getEmpName));

        return empIds.stream()
                .map(id -> nameMap.getOrDefault(id, id))
                .toList();
    }

    private String buildReceiversText(List<String> empIds) {
        if (empIds == null || empIds.isEmpty()) return "";

        // empIds -> nameMap (IN 한방)
        Map<String, String> nameMap = employeeRepository.findByEmpIdIn(empIds).stream()
                .collect(Collectors.toMap(Employee::getEmpId, Employee::getEmpName, (a, b) -> a));

        return empIds.stream()
                .map(id -> {
                    String nm = nameMap.getOrDefault(id, "");
                    String label = (nm == null || nm.isBlank()) ? "" : nm.trim();
                    return label.isBlank() ? id : (label + "(" + id + ")");
                })
                .distinct()
                .collect(Collectors.joining(", "));
    }
}