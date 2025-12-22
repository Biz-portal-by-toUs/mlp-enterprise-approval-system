package com.multi.mlpenterpriseapprovalsystem.document.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import com.multi.mlpenterpriseapprovalsystem.document.repository.DocumentRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
public class DocumentServiceV2 {
    private final DocumentRepositoryV2 documentRepositoryV2;

    // Http 요청의 status 파라미터에 따라 메서드 호출
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getDocumentsByStatus(String comId, String empId, ReqDocumentDto reqDocumentDto, String status, int page, String sort) {

        if("FINALIZED".equals(status)){ // status가 FINALIZED일때 최종승인된것들만 반환
            return getFinalizedDocuments(comId, reqDocumentDto, page, sort);
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }

    // Http 요청의 status 파라미터에 따라 메서드 호출
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMyDocumentsByStatus(String comId, String empId, ReqDocumentDto reqDocumentDto, String status, int page, String sort) {

        if("SUBMITTED".equals(status)){ // status가 SUBMITTED일때 내가 상신한 모든 문서 반환
            return getMySubmittedDocuments(comId, empId, reqDocumentDto, page, sort);
        }
        else if("AWAITING".equals(status)){ // status가 PENDING일때 내가 결재할 문서 반환
            return getAwaitingMyApprovalDocuments(comId, empId, reqDocumentDto, page, sort);
        }
        else if("PROCESSED".equals(status)){ // status가 PROCESSED일때 내가 결재한 문서 반환
            return getMyProcessedDocuments(comId, empId, reqDocumentDto, page, sort);
        }
        else{
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }
    }

    // 내 회사의 문서 중 내가 상신한 문서 조회
    // 문서상태는 검색창에서 미선택 기준(전체기준) 결재중(AW), 반려(RJ), 최종승인만(RI)조회
    // 내 결재상태는 내가 상신한 문서이기때문에 있을 수 없음. 내가 상신한 문서를 내가 결재하는건 불가능.
    // 상신일 기준 최신순(createdAt기준 LATEST인 SUBMIT_LATEST), 오래된순(createdAt기준 OLDEST인 SUBMIT_OLDEST)
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMySubmittedDocuments(String comId, String myEmpId, ReqDocumentDto req, int page, String sort) {

        Pageable pageable = PageRequest.of(page, 10);

        // 1. 문서상태 필터 처리: 미선택 시 AW(결재중), RJ(반려), FI(최종승인) 조회
        List<DocStat> docStats;
        if (req.getDocStat() != null && !req.getDocStat().isEmpty()) {
            docStats = List.of(DocStat.valueOf(req.getDocStat()));
        } else {
            docStats = List.of(DocStat.AW, DocStat.RJ, DocStat.FI);
        }

        // 2. 상세 검색 파라미터 정리 (Null-Safe)
        String docfoCatName = (req.getDocfoCatName() != null && !req.getDocfoCatName().isEmpty()) ? req.getDocfoCatName() : null;
        String writerDepName = (req.getWriterDepName() != null && !req.getWriterDepName().isEmpty()) ? req.getWriterDepName() : null;
        String docId = (req.getDocId() != null && !req.getDocId().isEmpty()) ? req.getDocId() : null;
        String docTitle = (req.getTitle() != null && !req.getTitle().isEmpty()) ? req.getTitle() : null;

        // 3. 정렬 기본값: 상신일 최신순 (SUBMIT_LATEST)
        String finalSort = (sort == null || sort.isEmpty()) ? "SUBMIT_LATEST" : sort;

        // 4. 리포지토리 호출
        Page<Document> documentPage = documentRepositoryV2.searchMySubmittedDocuments(
                comId,
                myEmpId,
                docfoCatName,
                writerDepName,
                docId,
                docTitle,
                finalSort,
                docStats,
                pageable
        );

        return documentPage.map(ResDocumentDto::toDto);
    }

    // 내 회사의 문서 중 내가 결재할 문서 조회
    // 상신일 기준 최신순(createdAt기준 LATEST인 SUBMIT_LATEST), 오래된순(createdAt기준 OLDEST인 SUBMIT_OLDEST)
    // 문서의 상태는 결재중(AW)여야만 함. 사용자는 검색창에서 문서상태를 선택할수없음(결재중인 AW고정)
    // 사용자가 검색창에서 내결재상태를 (내순서)ApprStat.I, (대기중)ApprStat.W만 선택가능
    // 사용자가 검색창에서 내결재상태를 미선택 시 내가 결재할 순서인ApprStat.I가 먼저 오고, 결재대기중인 ApprStat.W가 나중에 와야함.
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getAwaitingMyApprovalDocuments(String comId, String myEmpId, ReqDocumentDto req, int page, String sort) {

        Pageable pageable = PageRequest.of(page, 10);

        // 1. 문서 상태는 '결재중(AW)' 고정
        DocStat docStatFilter = DocStat.AW;

        // 2. 내 결재 상태 필터 처리: 미선택 시 I, W 전체 조회
        List<ApprStat> apprStats;
        if (req.getMyApprStat() != null && !req.getMyApprStat().isEmpty()) {
            apprStats = List.of(ApprStat.valueOf(req.getMyApprStat()));
        } else {
            apprStats = List.of(ApprStat.I, ApprStat.W);
        }

        // 3. 상세 검색 파라미터 정리 (Null-Safe)
        String docfoCatName = (req.getDocfoCatName() != null && !req.getDocfoCatName().isEmpty()) ? req.getDocfoCatName() : null;
        String writerDepName = (req.getWriterDepName() != null && !req.getWriterDepName().isEmpty()) ? req.getWriterDepName() : null;
        String docId = (req.getDocId() != null && !req.getDocId().isEmpty()) ? req.getDocId() : null;
        String docTitle = (req.getTitle() != null && !req.getTitle().isEmpty()) ? req.getTitle() : null;
        String writerId = (req.getWriterId() != null && !req.getWriterId().isEmpty()) ? req.getWriterId() : null;
        String writerName = (req.getWriterName() != null && !req.getWriterName().isEmpty()) ? req.getWriterName() : null;

        // 4. 리포지토리 호출
        Page<Document> documentPage = documentRepositoryV2.searchAwaitingMyApprovalDocuments(
                comId,
                myEmpId,
                docfoCatName,
                writerDepName,
                docId,
                docTitle,
                writerId,
                writerName,
                sort,
                docStatFilter,
                apprStats,
                pageable
        );

        // 해당 문서에 대한 나의 결재상태(결재중, 결재대기중)를 DTO에 매핑하여 반환
        return documentPage.map(doc -> ResDocumentDto.toDto(doc, myEmpId));
    }

    // 내 회사의 문서 중 내가 결재한 문서 조회. 결재자, 대직자 둘 다에게 보여야함
    // 문서상태는 검색창에서 미선택 기준(전체기준) 결재중(AW), 반려(RJ), 최종승인만(RI)조회.
    // 내 결재일 기준 최신순(endedAt기준 LATEST인 APPR_LATEST), 오래된순(endedAt기준 OLDEST인 APPR_OLDEST). 상신일 기준 최신순(createdAt기준 LATEST인 SUBMIT_LATEST), 오래된순(createdAt기준 OLDEST인 SUBMIT_OLDEST). 총 2개의 최신순, 2개의 오래된순으로 4개의 시간기준 정렬 있음.
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMyProcessedDocuments(String comId, String myEmpId, ReqDocumentDto req, int page, String sort) {

        Pageable pageable = PageRequest.of(page, 10);

        // 1. 문서상태 필터 처리: 미선택 시 AW, RJ, FI 전체 조회
        List<DocStat> docStats;
        if (req.getDocStat() != null && !req.getDocStat().isEmpty()) {
            docStats = List.of(DocStat.valueOf(req.getDocStat()));
        } else {
            docStats = List.of(DocStat.AW, DocStat.RJ, DocStat.FI);
        }

        // 2. 내 결재상태 필터 처리: 미선택 시 A, R 전체 조회
        List<ApprStat> apprStats;
        if (req.getMyApprStat() != null && !req.getMyApprStat().isEmpty()) {
            apprStats = List.of(ApprStat.valueOf(req.getMyApprStat()));
        } else {
            apprStats = List.of(ApprStat.A, ApprStat.R);
        }

        // 3. 상세 검색 파라미터 정리
        String docfoCatName = (req.getDocfoCatName() != null && !req.getDocfoCatName().isEmpty()) ? req.getDocfoCatName() : null;
        String writerDepName = (req.getWriterDepName() != null && !req.getWriterDepName().isEmpty()) ? req.getWriterDepName() : null;
        String docId = (req.getDocId() != null && !req.getDocId().isEmpty()) ? req.getDocId() : null;
        String docTitle = (req.getTitle() != null && !req.getTitle().isEmpty()) ? req.getTitle() : null;
        String writerId = (req.getWriterId() != null && !req.getWriterId().isEmpty()) ? req.getWriterId() : null;
        String writerName = (req.getWriterName() != null && !req.getWriterName().isEmpty()) ? req.getWriterName() : null;

        // 4. 정렬 기본값: 내 결재일 최신순(APPR_LATEST)
        String finalSort = (sort == null || sort.isEmpty()) ? "APPR_LATEST" : sort;

        // 5. 리포지토리 호출
        Page<Document> documentPage = documentRepositoryV2.searchMyProcessedDocuments(
                comId,
                myEmpId,
                docfoCatName,
                writerDepName,
                docId,
                docTitle,
                writerId,
                writerName,
                finalSort,
                docStats,
                apprStats,
                pageable
        );

        // 해당 문서에 대한 나의 결재상태(승인, 반려)를 DTO에 매핑하여 반환
        return documentPage.map(doc -> ResDocumentDto.toDto(doc, myEmpId));
    }

    // 내 회사의 최종승인문서 조회
    // 문서상태는 최종승인(FI)만 가능
    // 내 결재상태는 무관
    // 최종승인 기준 최신순(updatedAt기준 LATEST인 FINALIZED_LATEST), 오래된순(updatedAt기준 OLDEST인 FINALIZED_OLDEST). 상신일 기준 최신순(createdAt기준 LATEST인 SUBMIT_LATEST), 오래된순(createdAt기준 OLDEST인 SUBMIT_OLDEST).
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getFinalizedDocuments(String comId, ReqDocumentDto req, int page, String sort) {

        Pageable pageable = PageRequest.of(page, 10);

        // 1. 검색 필드 로컬 변수화 및 Null-Safe 처리
        DocStat docStatFilter1 = DocStat.FI;
        String docfoCatName = (req.getDocfoCatName() != null && !req.getDocfoCatName().isEmpty()) ? req.getDocfoCatName() : null;
        String writerDepName = (req.getWriterDepName() != null && !req.getWriterDepName().isEmpty()) ? req.getWriterDepName() : null;
        String docId = (req.getDocId() != null && !req.getDocId().isEmpty()) ? req.getDocId() : null;
        String docTitle = (req.getTitle() != null && !req.getTitle().isEmpty()) ? req.getTitle() : null;
        String writerId = (req.getWriterId() != null && !req.getWriterId().isEmpty()) ? req.getWriterId() : null;
        String writerName = (req.getWriterName() != null && !req.getWriterName().isEmpty()) ? req.getWriterName() : null;

        // 2. 정렬 기본값 처리: 최종승인일 최신순을 기본값으로 설정
        String finalSort = (sort == null || sort.isEmpty()) ? "FINALIZED_LATEST" : sort;

        // 3. 통합 필터 메서드 호출
        Page<Document> documentPage = documentRepositoryV2.searchFinalizedDocuments(
                comId,
                docfoCatName,
                writerDepName,
                docId,
                docTitle,
                writerId,
                writerName,
                finalSort,
                pageable,
                docStatFilter1
        );

        return documentPage.map(ResDocumentDto::toDto);
    }

}
