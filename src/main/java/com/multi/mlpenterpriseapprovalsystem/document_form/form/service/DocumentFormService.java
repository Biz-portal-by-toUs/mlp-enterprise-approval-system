package com.multi.mlpenterpriseapprovalsystem.document_form.form.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.ResDocumentFormDetailDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.ResDocumentFormListDto;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.DocumentFormStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocFormService
 * @since : 2025-12-22 월요일
 */

public interface DocumentFormService {

    Page<ResDocumentFormListDto> findListByStatuses(
            java.util.List<DocumentFormStats> stats,
            String comId,
            String keyword,
            Pageable pageable
    );

    ResDocumentFormDetailDto findDetailById(Long docfoNo, String comId);

    Long createDocumentForm(ReqDocumentFormCreateDto req, String comId, String writerId);

    Long updateDocumentForm(
            Long docfoNo,
            ReqDocumentFormCreateDto req,
            String comId,
            String writerId
    );

    // 삭제 플로우
    void requestDelete(Long docfoNo, String comId, CustomUser requester);
    void approveDelete(Long docfoNo, String comId);
    void rejectDelete(Long docfoNo, String comId, String rejectReason);

    // 승인/반려
    void changeApproveOrReject(Long docfoNo, String comId, DocumentFormStats next, String rejectReason);

    Long createTemp(ReqDocumentFormTempDto req, String comId, String writerId);

    void saveTemp(Long docfoNo, ReqDocumentFormTempDto req, String comId, String writerId);

    Page<ResDocumentFormListDto> findMyTempList(
            String comId,
            String writerId,
            String keyword,
            Pageable pageable
    );

    void deleteTemp(Long docfoNo, String comId, String writerId);
}