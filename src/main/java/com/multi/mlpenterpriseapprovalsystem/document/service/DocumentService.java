package com.multi.mlpenterpriseapprovalsystem.document.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqApprovalLineDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import com.multi.mlpenterpriseapprovalsystem.document.repository.ApprovalLineRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.DocumentRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDocumentFormCategoryRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDocumentFormRepository;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentFormCategory;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final TempDocumentFormRepository tempDocumentFormRepository;
    private final TempDocumentFormCategoryRepository tempDocumentFormCategoryRepository;

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
    // 상신일 기준 최신순(submittedAt기준 LATEST인 SUBMIT_LATEST), 오래된순(submittedAt기준 OLDEST인 SUBMIT_OLDEST)
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
        Page<Document> documentPage = documentRepository.searchMySubmittedDocuments(
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
    // 상신일 기준 최신순(submittedAt기준 LATEST인 SUBMIT_LATEST), 오래된순(submittedAt기준 OLDEST인 SUBMIT_OLDEST)
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
        Page<Document> documentPage = documentRepository.searchAwaitingMyApprovalDocuments(
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
    // 내 결재일 기준 최신순(endedAt기준 LATEST인 APPR_LATEST), 오래된순(endedAt기준 OLDEST인 APPR_OLDEST). 상신일 기준 최신순(submittedAt기준 LATEST인 SUBMIT_LATEST), 오래된순(submittedAt기준 OLDEST인 SUBMIT_OLDEST). 총 2개의 최신순, 2개의 오래된순으로 4개의 시간기준 정렬 있음.
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
        Page<Document> documentPage = documentRepository.searchMyProcessedDocuments(
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
    // 최종승인 기준 최신순(updatedAt기준 LATEST인 FINALIZED_LATEST), 오래된순(updatedAt기준 OLDEST인 FINALIZED_OLDEST). 상신일 기준 최신순(submittedAt기준 LATEST인 SUBMIT_LATEST), 오래된순(submittedAt기준 OLDEST인 SUBMIT_OLDEST).
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
        Page<Document> documentPage = documentRepository.searchFinalizedDocuments(
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

    // 문서식별자로 문서 상세조회
    @Transactional(readOnly = true)
    public ResDocumentDto getDocumentByDocNoWithStatus(String comId, String myEmpId, Long docNo, String status) {

        Document document;

        if ("SUBMITTED".equals(status)) { // 상신한 문서 상세 조회
            // 작성자가 나면서 문서상태가 AW인 문서 조회
            document = documentRepository.findSubmittedDoc(comId, docNo, myEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
        }
        else if ("AWAITING".equals(status)) { // 결재할 문서 상세 조회
            // 결재라인에 내가 있으면서 문서상태가 AW이면서 내 결재상태가 I or W인 문서 조회
            document = documentRepository.findAwaitingDoc(comId, docNo, myEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
        }
        else if ("PROCESSED".equals(status)) { // 결재한 문서 상세 조회
            // 결재라인에 내가 있으면서 문서상태가 AW or FI or RJ이면서 내 결재상태가 A or R인 문서 조회
            document = documentRepository.findProcessedDoc(comId, docNo, myEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
        }
        else if ("FINALIZED".equals(status)) { // 최종승인 문서 상세 조회
            // 문서상태가 FI인 문서 조회
            document = documentRepository.findFinalizedDoc(comId, docNo)
                    .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
        }
        else {
            throw new CustomException(ErrorCode.INVALID_DOCUMENT_STATUS_REQUEST);
        }

        return ResDocumentDto.toDto(document, myEmpId);
    }


    // 문서 생성
    public void createDocument(String comId, String myEmpId, ReqDocumentDto reqDocumentDto) {

        // 1. 연관 엔티티 조회
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Employee writer = employeeRepository.findByEmpId(myEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        DocumentFormCategory category = tempDocumentFormCategoryRepository.findById(reqDocumentDto.getDocfoCatNo())
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_CATEGORY_NOT_FOUND));

        DocumentForm form = tempDocumentFormRepository.findById(reqDocumentDto.getDocfoNo())
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        // 2. Document 생성 (docId는 null - 최종승인 시 발행)
        Document document = Document.toEntity(reqDocumentDto, company, writer, category, form);

        boolean isTemp = Boolean.TRUE.equals(reqDocumentDto.getTemp());

        if (isTemp) {
            document.saveAsTemp();  // temp=true, submittedAt=null, docStat=US
        } else {
            document.submit();      // temp=false, submittedAt=now, docStat=AW
        }

        documentRepository.save(document);

        // 4. 상신 or 임시저장 시 결재라인 검증 후 생성
        if (reqDocumentDto.getApprovalLines() != null && !reqDocumentDto.getApprovalLines().isEmpty()) {
            validateApprovalLineOrder(reqDocumentDto.getApprovalLines());
            createApprovalLines(document, company, reqDocumentDto.getApprovalLines(), isTemp);
        }

        log.info("문서 {} 완료: docNo={}, writer={}", isTemp ? "임시저장" : "상신", document.getDocNo(), myEmpId);
    }

    /**
     * 결재라인 순서 검증
     * - 결재 순서(seq)가 증가할수록 직급이 같거나 높아야 함 (posOrder가 같거나 작아야 함)
     * - 같은 직급 허용: 5→5→4→3→3 (O)
     * - 직급 역전 불가: 3→4→5 (X) - posOrder가 커지면 안 됨
     */
    private void validateApprovalLineOrder(List<ReqApprovalLineDto> lineDtos) {
        if (lineDtos == null || lineDtos.size() < 2) {
            return; // 결재자가 1명 이하면 검증 불필요
        }

        // seq 순서로 정렬
        List<ReqApprovalLineDto> sortedLines = lineDtos.stream()
                .sorted(Comparator.comparingInt(ReqApprovalLineDto::getSeq))
                .toList();

        Integer prevPosOrder = null;

        for (ReqApprovalLineDto lineDto : sortedLines) {
            Employee approver = employeeRepository.findByEmpId(lineDto.getApproverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            Integer currentPosOrder = approver.getPositions().getPosOrder();

            // 이전 결재자보다 직급이 낮으면 에러 (posOrder가 커지면 에러)
            // 같은 직급(posOrder 동일)은 허용
            if (prevPosOrder != null && currentPosOrder > prevPosOrder) {
                throw new CustomException(ErrorCode.INVALID_APPROVAL_LINE_ORDER);
            }

            prevPosOrder = currentPosOrder;
        }
    }

    // 결재라인 생성 (대직자 포함)
    private void createApprovalLines(Document document, Company company, List<ReqApprovalLineDto> lineDtos, boolean isTemp) {

        // ✅ seq 기준 정렬하여 순서 보장
        List<ReqApprovalLineDto> sortedLines = lineDtos.stream()
                .sorted(Comparator.comparingInt(ReqApprovalLineDto::getSeq))
                .toList();

        for (int i = 0; i < lineDtos.size(); i++) {
            ReqApprovalLineDto lineDto = lineDtos.get(i);

            Employee approver = employeeRepository.findByEmpId(lineDto.getApproverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            // 첫 번째 결재자는 I(결재중), 나머지는 W(대기)
            ApprStat apprStat;
            if (isTemp) {
                apprStat = ApprStat.W;
            } else {
                apprStat = (i == 0) ? ApprStat.I : ApprStat.W;
            }

            // 결재자 추가
            ApprovalLine approverLine = ApprovalLine.toEntity(
                    document,
                    approver,
                    company,
                    lineDto.getSeq(),
                    apprStat,
                    false  // isDelegate = false
            );
            approvalLineRepository.save(approverLine);

            // 결재자가 휴가중(v)이고 대직자가 있으면 대직자도 추가
            if ("V".equals(approver.getAtte()) && approver.getDelegate() != null) {
                ApprovalLine delegateLine = ApprovalLine.toEntity(
                        document,
                        approver.getDelegate(),
                        company,
                        lineDto.getSeq(),  // 같은 seq
                        apprStat,          // 같은 상태
                        true               // isDelegate = true
                );
                approvalLineRepository.save(delegateLine);

                log.info("대직자 추가: 결재자={}, 대직자={}, seq={}",
                        approver.getEmpId(), approver.getDelegate().getEmpId(), lineDto.getSeq());
            }
        }
    }



    // 문서코드(docId) 생성
    // 문서가 최종승인되어야 발급
    // 회사약어 최대3자리(comId) + 부서코드 최대3자리(depId) + 년도4자리 + 일련번호 4자리 = 최대 총 14자리
    // 현재는 가짜 데이터 넣어놔서 14자리 넘음
//    private String generateDocId(String comId) {
//        String newDocId = comId +
//    }
}
