package com.multi.mlpenterpriseapprovalsystem.document.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import com.multi.mlpenterpriseapprovalsystem.document.repository.DocumentRepositoryV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문서 서비스 관리
 *
 * @author : 이지헌
 * @filename : DocumentService
 * @since : 25. 12. 15. 월요일
 */

@Transactional
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceV1 {
    private final DocumentRepositoryV1 documentRepositoryV1;


    // Http 요청의 status 파라미터에 따라 메서드 호출
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getDocumentsByStatus(String comId, String empId, String status, int page, String sort) {

        if("FINALIZED".equals(status)){ // status가 FINALIZED일때 최종승인된것들만 반환
            return getFinalizedDocuments(comId, page, sort);
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }

    // Http 요청의 status 파라미터에 따라 메서드 호출
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMyDocumentsByStatus(String comId, String empId, String status, int page, String sort) {

        if("SUBMITTED".equals(status)){ // status가 SUBMITTED일때 내가 상신한 모든 문서 반환
            return getMySubmittedDocuments(comId, empId, page, sort);
        }
        else if("AWAITING".equals(status)){ // status가 PENDING일때 내가 결재할 문서 반환
            return getAwaitingMyApprovalDocuments(comId, empId, page);
        }
        else if("PROCESSED".equals(status)){ // status가 PROCESSED일때 내가 결재한 문서 반환
            return getMyProcessedDocuments(comId, empId, page, sort);
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }

    // 내 회사의 문서 중 내가 상신한 문서 조회
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMySubmittedDocuments(String comId, String empId, int page, String sort) {

        Page<Document> documentPage;
        Pageable pageable;

        if("OLDEST".equals(sort)){
            // 상신일 기준 오래된순
            pageable = PageRequest.of(page, 10, Sort.by("createdAt").ascending());
            documentPage = documentRepositoryV1.getMySubmittedDocuments(comId, empId, pageable);
        }
        else if("AWAITING".equals(sort)){
            // 결재중인 문서가 상신일 기준 최신순으로 위에 오게. 나머지는 최신순
            pageable = PageRequest.of(page, 10);
            documentPage = documentRepositoryV1.getMySubmittedDocumentsByDocStat(comId, empId, DocStat.AW, pageable);
        }
        else if("FINALIZED".equals(sort)){
            // 최종승인된 문서가 상신일 기준 최신순으로 위에 오게. 나머지는 최신순
            pageable = PageRequest.of(page, 10);
            documentPage = documentRepositoryV1.getMySubmittedDocumentsByDocStat(comId, empId, DocStat.FI, pageable);
        }
        else if("REJECTED".equals(sort)){
            // 반려된 문서가 상신일 기준 최신순으로 위에 오게. 나머는 최신순
            pageable = PageRequest.of(page, 10);
            documentPage = documentRepositoryV1.getMySubmittedDocumentsByDocStat(comId, empId, DocStat.RJ, pageable);
        }
        else if("LATEST".equals(sort)){
            // 상신일 기준 최신순
            pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
            documentPage = documentRepositoryV1.getMySubmittedDocuments(comId, empId, pageable);
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_SORT_REQUEST);
        }

        return documentPage.map(ResDocumentDto::toDto);
    }

    // 내 회사의 문서 중 내가 결재할 문서 조회
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getAwaitingMyApprovalDocuments(String comId, String empId, int page) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Document> documentPage = documentRepositoryV1.getAwaitingMyApprovalDocuments(comId, empId, ApprStat.I, ApprStat.W, DocStat.AW, pageable);

        // 해당 문서에 대한 나의 결재상태(결재중, 결재대기중)설정하여 반환
        return documentPage.map(doc -> ResDocumentDto.toDto(doc, empId));
    }

    // 내 회사의 문서 중 내가 결재한 문서 조회. 결재자, 대직자 둘 다에게 보여야함
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMyProcessedDocuments(String comId, String empId, int page, String sort) {

        Page<Document> documentPage;
        Pageable pageable = PageRequest.of(page, 10);;

        if("OLDEST".equals(sort)){
            // 내 결재일 기준 오래된순(남의 결재일은 반영안됨)
            documentPage = documentRepositoryV1.getMyProcessedDocumentsOldest(comId, empId, ApprStat.A, ApprStat.R, pageable);
        }
        else if("APPROVED".equals(sort)){
            // 내가 승인한 문서가 내 결재일 기준 최신순으로 위에 오게. 나머지는 내 결재일 기준 최신순
            documentPage = documentRepositoryV1.getMyProcessedDocumentsByMyApprStat(comId, empId, ApprStat.A, ApprStat.R, ApprStat.A, pageable);
        }
        else if("REJECTED".equals(sort)){
            // 내가 반려한 문서가 내 결재일 기준 최신순으로 위에 오게. 나머지는 내 결재일 기준 최신순
            documentPage = documentRepositoryV1.getMyProcessedDocumentsByMyApprStat(comId, empId, ApprStat.A, ApprStat.R, ApprStat.R, pageable);
        }
        else if("LATEST".equals(sort)){
            // 내 결재일 기준 최신순(남의 결재일은 반영 안됨)
            documentPage = documentRepositoryV1.getMyProcessedDocumentsLatest(comId, empId, ApprStat.A, ApprStat.R, pageable);
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_SORT_REQUEST);
        }

        // 해당 문서에 대한 나의 결재상태(승인, 반려)설정하여 반환
        return documentPage.map(doc -> ResDocumentDto.toDto(doc, empId));
    }

    // 내 회사의 최종승인문서 조회
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getFinalizedDocuments(String comId, int page, String sort) {

        Sort sorting;
        if ("OLDEST".equals(sort)) {
            // OLDEST면 문서변경일자 기준 오래된 순
            sorting = Sort.by("updatedAt").ascending();
        } else if ("LATEST".equals(sort)) {
            // LATEST 또는 null이면 문서변경일자 기준 최신순
            sorting = Sort.by("updatedAt").descending();
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_SORT_REQUEST);
        }

        Pageable pageable = PageRequest.of(page, 10, sorting);
        Page<Document> documentPage = documentRepositoryV1.getFinalizedDocuments(comId, DocStat.FI, pageable);

        return documentPage.map(ResDocumentDto::toDto);
    }

}
