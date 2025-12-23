package com.multi.mlpenterpriseapprovalsystem.document_form.form.service;

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
    Page<ResDocumentFormListDto> findListByStatus(
            DocumentFormStats stat,
            Pageable pageable
    );

    ResDocumentFormDetailDto findDetailById(Long docfoNo);

    //Long createDocumentForm(ReqDocumentFormCreateDto req);

    void deleteDocumentForm(Long docfoNo);

    //Long updateDocumentForm(Long docfoNo, ReqDocumentFormCreateDto req);
}