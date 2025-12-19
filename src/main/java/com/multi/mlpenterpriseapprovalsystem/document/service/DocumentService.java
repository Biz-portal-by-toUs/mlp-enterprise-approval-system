package com.multi.mlpenterpriseapprovalsystem.document.service;

import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class DocumentService {
    private final DocumentRepository documentRepository;


    // Http 요청의 status 파라미터에 따라 메서드 호출
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getDocumentsByStatus(String comId, String empId, String status, int page) {

        if(status.equals("FINALIZED")){ // status가 FINALIZED일때 최종승인된것들만 반환
            return getApprovedDocuments(comId, page);
        }
        else if(status.equals("SUBMITTED")){ // status가 SUBMITTED일때 내가 상신한 모든 문서 반환
            return getMySubmittedDocuments(comId, empId, page);
        }
        else if(status.equals("AWAITING")){ // status가 PENDING일때 내가 결재할 문서 반환
            return getDocumentsAwaitingMyApproval(comId, empId, page);
        }
        else if(status.equals("PROCESSED")){
            return getMyProcessedDocuments(comId, empId, page);
        }

        return getDocumentsAwaitingMyApproval(comId, empId, page);
    }

    // 내가 결재한 문서 반환(결재자, 대직자 둘 다에게 반환)
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMyProcessedDocuments(String comId, String empId, int page) {
        Pageable pageable = PageRequest.of(page, 10); // 최근 내가 결재한게 상위에 오도록

        // 결재라인에 내가 있고 승인 or 반려한 문서들 반환
        Page<Document> documentPage = documentRepository.getMyProcessedDocuments(comId, empId, pageable);

        // 해당 문서에 대한 나의 결재상태(승인, 반려)설정하여 반환
        return documentPage.map(doc -> ResDocumentDto.toDto(doc, empId));
    }

    // 내가 결재할 문서 반환
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getDocumentsAwaitingMyApproval(String comId, String empId, int page) {
        Pageable pageable = PageRequest.of(page, 10);

        Page<Document> documentPage = documentRepository.getDocumentsAwaitingMyApproval(comId, empId, pageable);

        // 해당 문서에 대한 나의 결재상태(결재중, 결재대기중)설정하여 반환
        return documentPage.map(doc -> ResDocumentDto.toDto(doc, empId));
    }

    // 내가 상신한 문서 반환
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMySubmittedDocuments(String comId, String empId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());

        Page<Document> documentPage = documentRepository.getMySubmittedDocuments(comId, empId, pageable);

        return documentPage.map(ResDocumentDto::toDto);
    }

    // 최종승인 문서만 반환
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getApprovedDocuments(String comId, int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("updatedAt").descending());

        List<Document> documents = documentRepository.findAllWithApprovalLinesByComId(comId);

        // 최종 승인된 문서만 필터링
        List<ResDocumentDto> approvedDocs = new ArrayList<>();

        for (Document document : documents) {
            if (isFullyApproved(document)) {
                approvedDocs.add(ResDocumentDto.toDto(document));
            }
        }

        // 수동 페이징 처리
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), approvedDocs.size());

        List<ResDocumentDto> pagedList;
        if (start > approvedDocs.size()) {
            pagedList = new ArrayList<>();
        } else {
            pagedList = approvedDocs.subList(start, end);
        }

        return new PageImpl<>(pagedList, pageable, approvedDocs.size());
    }


    /**
     * 문서가 최종 승인되었는지 체크
     * 결재순서 1~5까지 돌면서 각 순서별로 승인 여부 확인
     * 같은 순서에 대직자가 있으면 둘 중 하나라도 A면 승인
     */
    private boolean isFullyApproved(Document document) {
        List<ApprovalLine> approvalLines = document.getApprovalLines();

        if (approvalLines == null || approvalLines.isEmpty()) {
            return false;
        }

        // 결재순서별로 그룹핑 (seq -> List<ApprovalLine>)
        Map<Integer, List<ApprovalLine>> linesBySeq = new HashMap<>();
        int maxSeq = 0;

        for (ApprovalLine line : approvalLines) {
            int seq = line.getSeq();

            // 해당 seq 키가 없으면 새 리스트 생성
            if (!linesBySeq.containsKey(seq)) {
                linesBySeq.put(seq, new ArrayList<>());
            }
            linesBySeq.get(seq).add(line);

            // 최대 순서 갱신
            if (seq > maxSeq) {
                maxSeq = seq;
            }
        }

        // 1번부터 마지막 순서까지 체크
        for (int seq = 1; seq <= maxSeq; seq++) {
            List<ApprovalLine> linesAtSeq = linesBySeq.get(seq);

            // 해당 순서에 결재라인이 없으면 false
            if (linesAtSeq == null || linesAtSeq.isEmpty()) {
                return false;
            }

            // 해당 순서에서 하나라도 승인(A)이 있는지 체크
            boolean isApprovedAtSeq = false;
            for (ApprovalLine line : linesAtSeq) {
                if (line.getApprStat() == ApprStat.A) {
                    isApprovedAtSeq = true;
                    break;
                }
            }

            // 해당 순서가 승인 안됐으면 최종 승인 아님
            if (!isApprovedAtSeq) {
                return false;
            }
        }

        // 모든 순서가 승인됨 = 최종 승인
        return true;
    }
}
