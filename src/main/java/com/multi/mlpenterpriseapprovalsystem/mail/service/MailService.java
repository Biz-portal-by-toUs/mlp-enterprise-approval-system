package com.multi.mlpenterpriseapprovalsystem.mail.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.req.ReqMailSendDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailDetailDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailListDto;
import com.multi.mlpenterpriseapprovalsystem.mail.dto.res.ResMailSendDto;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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


    Page<ResMailListDto> getInbox(CustomUser user, Pageable pageable);

    Page<ResMailListDto> getInboxByRoles(String userEmpId, List<MailRole> roles, Pageable pageable); // 추후 확장 시 사용

    Page<ResMailListDto> getSent(String senderEmpId, Pageable pageable);

    ResMailDetailDto getDetail(String mailId, String viewerEmpId);

    Page<ResMailListDto> getTrash(String userEmpId, Pageable pageable);

    void markAsRead(String mailId, String userEmpId);

    void moveToTrash(String mailId, String userEmpId);

    void restoreFromTrash(String mailId, String userEmpId);

    void purge(String mailId, String userEmpId); // 휴지통 거친 후만 (row 삭제)
}
