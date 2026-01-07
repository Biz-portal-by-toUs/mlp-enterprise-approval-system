package com.multi.mlpenterpriseapprovalsystem.mail.service;

import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;
import org.springframework.data.domain.*;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : MailService
 * @since : 2025-12-30 화요일
 */
public interface MailService {
    //메일 전송
    ResMailSendDto sendMail(String senderEmpId, ReqMailSendDto req);

    Page<ResMailListDto> getInbox(String userEmpId, MailRole role, String q, Pageable pageable);

    Page<ResMailListDto> getSent(String senderEmpId, String q, Pageable pageable);

    ResMailDetailDto getDetail(String mailId, String viewerEmpId);

    Page<ResMailListDto> getTrash(String userEmpId, Pageable pageable);

    void markAsRead(String mailId, String userEmpId);

    void moveToTrash(String mailId, String userEmpId);

    void restoreFromTrash(String mailId, String userEmpId);

    void purge(String mailId, String userEmpId); // 휴지통 거친 후만 (row 삭제)

    void setPrior(String mailId, String userEmpId, boolean prior);

    void togglePrior(String mailId, String userEmpId);

    // 임시저장 생성/수정
    ResMailDraftSavedDto saveDraft(String senderEmpId, ReqMailDraftSaveDto req);

    // 임시저장 목록
    Page<ResMailListDto> getDrafts(String senderEmpId, String q, Pageable pageable);

    // 임시저장 상세 (발송 화면에서 불러오기)
    ResMailDetailDto getDraftDetail(String mailId, String senderEmpId);

    // 임시저장 삭제 (완전 삭제)
    void deleteDraft(String mailId, String senderEmpId);

    // 임시저장 -> 발송 (초안 불러와서 보내기)
    ResMailSendDto sendDraft(String mailId, String senderEmpId, ReqMailDraftSendDto req);
}
