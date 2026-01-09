package com.multi.mlpenterpriseapprovalsystem.mail.service;

import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import org.springframework.data.domain.*;

import java.time.*;

public interface MailService {

    ResMailSendDto sendMail(String senderEmpId, ReqMailSendDto req);

    Page<ResMailListDto> getInbox(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable);

    Page<ResMailListDto> getSent(String senderEmpId, String q, LocalDate from, LocalDate to, Pageable pageable);

    Page<ResMailListDto> getSelfMailbox(String userEmpId, String q, LocalDate from, LocalDate to, Pageable pageable);

    Page<ResMailListDto> getTrash(String userEmpId, Pageable pageable);

    // 상세/상태: mailNo 기준
    ResMailDetailDto getDetail(Long mailNo, String viewerEmpId);

    void markAsRead(Long mailNo, String userEmpId);

    void moveToTrash(Long mailNo, String userEmpId);

    void restoreFromTrash(Long mailNo, String userEmpId);

    void purge(Long mailNo, String userEmpId);

    void setPrior(Long mailNo, String userEmpId, boolean prior);

    void togglePrior(Long mailNo, String userEmpId);

    // ===== drafts =====
    ResMailDraftSavedDto saveDraft(String senderEmpId, ReqMailDraftSaveDto req);

    Page<ResMailListDto> getDrafts(String senderEmpId, String q, Pageable pageable);

    ResMailDraftDetailDto getDraftDetail(String mailId, String senderEmpId);

    void deleteDraft(String mailId, String senderEmpId);

    ResMailSendDto sendDraft(String mailId, String senderEmpId, ReqMailDraftSendDto req);

    ResMailReplyPayloadDto getReplyPayload(Long mailNo, String viewerEmpId);

    long countUnreadInbox(String empId);
}