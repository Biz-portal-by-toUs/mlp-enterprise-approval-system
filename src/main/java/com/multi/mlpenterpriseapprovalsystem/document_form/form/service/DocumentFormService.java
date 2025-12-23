package com.multi.mlpenterpriseapprovalsystem.document_form.form.service;

import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.*;
import org.springframework.data.domain.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocFormService
 * @since : 2025-12-22 월요일
 */

public interface DocumentFormService {
    Page<DocumentFormListResDto> findListByStatus(
            DocumentFormStats stat,
            Pageable pageable
    );

    DocumentFormDetailResDto findDetailById(Long docfoNo);

    Long createDocumentForm(DocumentFormCreateReqDto req);

    void deleteDocumentForm(Long docfoNo);

    Long updateDocumentForm(Long docfoNo, DocumentFormCreateReqDto req);
}