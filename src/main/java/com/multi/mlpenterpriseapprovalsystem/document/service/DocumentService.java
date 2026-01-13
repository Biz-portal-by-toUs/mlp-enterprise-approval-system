package com.multi.mlpenterpriseapprovalsystem.document.service;

import com.multi.mlpenterpriseapprovalsystem.attendance.service.AttendanceService;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.document.config.DocumentOpenAiConfig;
import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqDocOpenAiDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqApprovalLineDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.req.ReqDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocOpenAiDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.ResDocumentDto;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import com.multi.mlpenterpriseapprovalsystem.document.repository.ApprovalLineRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.DocumentRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDocumentFormCategoryRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDocumentFormRepository;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentFormCategory;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.service.SearchOutboxAppender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 문서 관리 서비스
 *
 * @author : 이지헌
 * @filename : DocumentService
 * @since : 25. 12. 15. 월요일
 */

@Transactional
@Slf4j
@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final TempDocumentFormRepository tempDocumentFormRepository;
    private final TempDocumentFormCategoryRepository tempDocumentFormCategoryRepository;
    private final DocumentOpenAiConfig documentOpenAiConfig;
    private final WebClient documentOpenAiWebClient;
    private final AttendanceService attendanceService;
    private final NotificationService notificationService;
    private final SearchOutboxAppender searchOutboxAppender;

    public DocumentService(
            DocumentRepository documentRepository,
            ApprovalLineRepository approvalLineRepository,
            CompanyRepository companyRepository,
            EmployeeRepository employeeRepository,
            TempDocumentFormRepository tempDocumentFormRepository,
            TempDocumentFormCategoryRepository tempDocumentFormCategoryRepository,
            DocumentOpenAiConfig documentOpenAiConfig,
            @Qualifier("documentOpenAiWebClient") WebClient documentOpenAiWebClient,
            AttendanceService attendanceService, NotificationService notificationService,
            SearchOutboxAppender searchOutboxAppender
    ) {
        this.documentRepository = documentRepository;
        this.approvalLineRepository = approvalLineRepository;
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.tempDocumentFormRepository = tempDocumentFormRepository;
        this.tempDocumentFormCategoryRepository = tempDocumentFormCategoryRepository;
        this.documentOpenAiConfig = documentOpenAiConfig;
        this.documentOpenAiWebClient = documentOpenAiWebClient;
        this.attendanceService = attendanceService;
        this.notificationService = notificationService;
        this.searchOutboxAppender= searchOutboxAppender;
    }

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

        if("UNSUBMITTED".equals(status)){
            return getMyUnSubmittedDocuments(comId, empId, page);
        }
        else if("SUBMITTED".equals(status)){ // status가 SUBMITTED일때 내가 상신한 모든 문서 반환
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

    // 내 회사의 문서 중 내가 임시저장한 문서 조회
    @Transactional(readOnly = true)
    public Page<ResDocumentDto> getMyUnSubmittedDocuments(String comId, String myEmpId, int page) {

        Pageable pageable = PageRequest.of(page, 10);

        // 문서상태가 US(상신전)여야 함
        DocStat docStatFilter1 = DocStat.US;

        Page<Document> documentPage = documentRepository.searchMyUnSubmittedDocuments(
                comId,
                myEmpId,
                docStatFilter1,
                pageable
        );

        return documentPage.map(ResDocumentDto::toDto);
    }

    // 내 회사의 문서 중 내가 상신한 문서 조회
    // 문서상태는 검색창에서 미선택 기준(전체기준) 결재중(AW), 반려(RJ), 최종승인만(RI)조회
    // 내 결재상태는 내가 상신한 문서이기때문에 있을 수 없음. 내가 상신한 문서를 내가 결재하는건 불가능.
    // 최근 결재일 기준 최신순(endedAt기준 LATEST인 APPR_LATEST), 오래된순(endedAt기준 OLDEST인 APPR_OLDEST)
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

        if("UNSUBMITTED".equals(status)){
            document = documentRepository.findUnSubmittedDoc(comId, docNo, myEmpId)
                    .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
        }
        else if ("SUBMITTED".equals(status)) { // 상신한 문서 상세 조회
            // 작성자가 나면서 문서상태가 AW or RJ or FI인 문서 조회
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


    // 문서 상신 및 임시저장
    public Long createDocument(String comId, String myEmpId, ReqDocumentDto reqDocumentDto) {

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
            validateApproverNotSelf(myEmpId, reqDocumentDto.getApprovalLines());
            validateApprovalLineOrder(reqDocumentDto.getApprovalLines());
            createApprovalLines(document, company, reqDocumentDto.getApprovalLines(), isTemp);

            if (!isTemp) {
                notifyApprovers(document, 1); // 1번 순서 결재자들에게 알림
            }
        }

        log.info("문서 {} 완료: docNo={}, writer={}", isTemp ? "임시저장" : "상신", document.getDocNo(), myEmpId);

        return document.getDocNo();
    }

    /**
     * 결재라인에 본인(작성자)이 포함되어 있는지 검증
     */
    private void validateApproverNotSelf(String writerEmpId, List<ReqApprovalLineDto> lineDtos) {
        boolean hasSelf = lineDtos.stream()
                .anyMatch(line -> writerEmpId.equals(line.getApproverId()));

        if (hasSelf) {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_LINE_SELF);
        }
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

    private void createApprovalLines(Document document, Company company, List<ReqApprovalLineDto> lineDtos, boolean isTemp) {
        List<ReqApprovalLineDto> sortedLines = lineDtos.stream()
                .sorted(Comparator.comparingInt(ReqApprovalLineDto::getSeq))
                .toList();

        for (ReqApprovalLineDto lineDto : sortedLines) {
            Employee approver = employeeRepository.findByEmpId(lineDto.getApproverId())
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            ApprStat apprStat = isTemp ? ApprStat.W : (lineDto.getSeq() == 1 ? ApprStat.I : ApprStat.W);

            // 1. 원 결재자 추가
            ApprovalLine approverLine = ApprovalLine.toEntity(document, approver, company, lineDto.getSeq(), apprStat, false, null);
            approvalLineRepository.save(approverLine);

            // 2. 대직자 체인 추적 (순환 참조 방지 로직 추가)
            Employee currentTarget = approver;
            Set<String> visitedEmpIds = new HashSet<>();
            visitedEmpIds.add(approver.getEmpId()); // 시작 결재자 기록

            while ("V".equals(currentTarget.getAtte()) && currentTarget.getDelegate() != null) {
                Employee delegate = currentTarget.getDelegate();
                String delegateId = delegate.getEmpId();

                // 순환 참조 확인
                if (visitedEmpIds.contains(delegateId)) {
                    log.error("대직자 순환 참조 감지: {} <-> {}", currentTarget.getEmpId(), delegateId);
                    break; // 또는 비즈니스 로직에 따라 CustomException 발생
                }

                ApprovalLine delegateLine = ApprovalLine.toEntity(document, delegate, company, lineDto.getSeq(), apprStat, true, currentTarget);
                approvalLineRepository.save(delegateLine);

                visitedEmpIds.add(delegateId); // 방문 기록
                currentTarget = delegate;
            }
        }
    }

    // 상신 취소
    public void cancelSubmit(String comId, String myEmpId, Long docNo) {
        // 문서 조회
        Document document = documentRepository.findById(docNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 본인이 작성한 문서인지 확인
        if (!document.getWriter().getEmpId().equals(myEmpId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 회사 일치 확인
        if (!document.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 결재 진행 여부 확인 (A 또는 R 상태인 결재자가 있으면 취소 불가)
        List<ApprovalLine> approvalLines = approvalLineRepository.findByDocument_docNo(docNo);

        boolean hasProcessedApproval = approvalLines.stream()
                .anyMatch(line -> line.getApprStat() == ApprStat.A || line.getApprStat() == ApprStat.R);

        if (hasProcessedApproval) {
            throw new CustomException(ErrorCode.DOCUMENT_ALREADY_PROCESSED);
        }

        // 6. 문서 상태만 변경 (결재라인 유지)
        document.cancelSubmit();

        log.info("상신 취소 완료: docNo={}, writer={}", docNo, myEmpId);
    }

    // 결재 승인 및 반려
    public void processApproval(String comId, String myEmpId, Long docNo, ReqApprovalLineDto reqDto) {

        // 1. 문서 조회
        Document document = documentRepository.findById(docNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        Employee currentApprover = employeeRepository.findByEmpId(myEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // 2. 회사 일치 확인
        if (!document.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 3. 문서 상태 확인 (결재중인 문서만 처리 가능)
        if (document.getDocStat() != DocStat.AW) {
            throw new CustomException(ErrorCode.DOCUMENT_NOT_AWAITING);
        }

        // 4. 결재라인에서 내 결재라인 조회 (본인이 결재자이거나 대직자인 경우)
        List<ApprovalLine> allLines = approvalLineRepository.findByDocument_docNo(document.getDocNo());

        List<ApprovalLine> myLines = allLines.stream()
                .filter(line -> line.getApprover().getEmpId().equals(myEmpId)
                        && line.getApprStat() == ApprStat.I)
                .toList();

        if (myLines.isEmpty()) {
            throw new CustomException(ErrorCode.NOT_MY_TURN_TO_APPROVE);
        }

        // 6. 승인/반려 처리
        // 내 라인들 중 첫 번째의 seq를 기준으로 잡음 (모두 같은 seq임)
        int currentSeq = myLines.get(0).getSeq();
        String apprStat = reqDto.getApprStat();

        if ("A".equals(apprStat)) {
            // 승인 처리
            myLines.forEach(ApprovalLine::approve);

            // 같은 seq의 다른 결재자(원 결재자 또는 대직자)도 승인 처리 (isActualAppr = false)
            markOtherApproversInSameSeq(allLines, currentSeq, myEmpId, ApprStat.A, null);

            // 다음 결재자에게 차례 넘기기
            passToNextApprover(allLines, currentSeq);

            // 모든 결재자가 승인했는지 확인 후 문서 상태 변경
            boolean allApproved = allLines.stream()
                    .allMatch(line -> line.getApprStat() == ApprStat.A);

            if (allApproved) {
                String docId = generateDocId(document);
                document.finalize(docId);

                searchOutboxAppender.enqueueUpsert(comId, SearchDocType.APPROVAL, document.getDocNo());
                log.info("문서 최종 승인: docNo={}, docId={}", docNo, docId);

                // 문서양식이 휴가 신청서, 출장 신청서이면 호출
                attendanceService.processAttendance(document);

                notifyWriter(document, "[결재 승인]",
                        currentApprover.getEmpName() + "님이 '" + document.getTitle() + "' 문서를 최종 승인하였습니다.");
            }
        } else if ("R".equals(apprStat)) {
            // 반려 처리
            if (reqDto.getRejReason() == null || reqDto.getRejReason().trim().isEmpty()) {
                throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED);
            }

            myLines.forEach(line -> line.reject(reqDto.getRejReason()));

            // 같은 seq의 다른 결재자(원 결재자 또는 대직자)도 반려 처리 (isActualAppr = false)
            markOtherApproversInSameSeq(allLines, currentSeq, myEmpId, ApprStat.R, reqDto.getRejReason());

            document.reject();

            log.info("문서 반려: docNo={}, rejector={}, reason={}", docNo, myEmpId, reqDto.getRejReason());

            notifyWriter(document, "[결재 반려]",
                    currentApprover.getEmpName() + "님이 '" + document.getTitle() + "' 문서를 반려하였습니다. 사유: " + reqDto.getRejReason());
        } else {
            throw new CustomException(ErrorCode.INVALID_APPROVAL_STATUS);
        }

        log.info("결재 처리 완료: docNo={}, approver={}, status={}", docNo, myEmpId, apprStat);
    }

    /**
     * 같은 seq의 다른 결재자 상태 변경 (isActualAppr = false)
     * - 원 결재자가 결재하면 대직자도 같이 처리
     * - 대직자가 결재하면 원 결재자도 같이 처리
     */
    private void markOtherApproversInSameSeq(List<ApprovalLine> allLines, int seq, String actualApproverId, ApprStat status, String rejReason) {
        allLines.stream()
                .filter(line -> line.getSeq() == seq && !line.getApprover().getEmpId().equals(actualApproverId))
                .forEach(line -> {
                    line.markAsNotActualApprover(status, rejReason);
                });
    }

    /**
     * 다음 결재자에게 차례 넘기기
     * 같은 seq에 원 결재자와 대직자가 있으면 둘 다 I로 변경
     */
    private void passToNextApprover(List<ApprovalLine> allLines, int currentSeq) {
        // 다음 seq의 결재자 찾기 (W 상태인 결재자 중 가장 작은 seq)
        OptionalInt nextSeqOpt = allLines.stream()
                .filter(line -> line.getSeq() > currentSeq && line.getApprStat() == ApprStat.W)
                .mapToInt(ApprovalLine::getSeq)
                .min();

        if (nextSeqOpt.isPresent()) {
            int nextSeq = nextSeqOpt.getAsInt();

            // 해당 seq의 모든 결재자를 I로 변경 (원 결재자 + 대직자 모두)
            allLines.stream()
                    .filter(line -> line.getSeq() == nextSeq && line.getApprStat() == ApprStat.W)
                    .forEach(ApprovalLine::setInProgress);

            // 다음 순번 결재자들에게 알림 발송
            Document doc = allLines.get(0).getDocument();
            notifyApprovers(doc, nextSeq);
        }
    }

    // 문서코드(docId) 생성
    // 문서가 최종승인되어야 발급
    // 회사약어 최대3자리(comId) + 부서코드 최대3자리(depId) + 년도4자리 + 일련번호 4자리 = 최대 총 14자리
    /**
     * 문서코드(docId) 생성
     * 형식: 회사코드(3) + 부서코드(3) + 년도(4) + 일련번호(4) = 최대 14자리
     * 예시: C01D012024A001
     *
     * 동시성 문제 해결: 비관적 락(Pessimistic Lock) 사용
     */
    public String generateDocId(Document document) {
        String comId = document.getCompany().getComId();
        String depId = document.getWriter().getDepartment().getDepId();
        String year = String.valueOf(LocalDateTime.now().getYear());

        // 접두사: 회사코드 + 부서코드 + 년도
        String prefix = comId + depId + year;

        // 해당 접두사로 시작하는 가장 마지막 문서코드 조회 (비관적 락)
        String lastDocId = documentRepository.findLastDocIdByPrefixWithLock(prefix);

        String newSerial;
        if (lastDocId == null) {
            // 해당 년도 첫 문서
            newSerial = "0001";
        } else {
            // 마지막 4자리 추출 후 +1
            String lastSerial = lastDocId.substring(lastDocId.length() - 4);
            newSerial = incrementSerial(lastSerial);
        }

        return prefix + newSerial;
    }

    /**
     * 일련번호 증가 (36진수: 0-9, A-Z)
     * 0001 -> 0002 -> ... -> 9999 -> 000A -> 000B -> ... -> ZZZZ
     */
    private String incrementSerial(String serial) {
        // 36진수로 변환 (0-9, A-Z)
        int value = Integer.parseInt(serial, 36);
        value++;

        if (value > 1679615) { // ZZZZ = 1679615 (36진수)
            throw new CustomException(ErrorCode.DOCUMENT_SERIAL_OVERFLOW);
        }

        // 다시 36진수 문자열로 변환 후 4자리 패딩
        String newSerial = Integer.toString(value, 36).toUpperCase();
        return String.format("%4s", newSerial).replace(' ', '0');
    }

    /**
     * 반려된 문서 재작성 (새 문서 생성)
     * - 기존 반려 문서는 그대로 유지하되 isResubmitted = true, resubmittedFor = 새 문서로 변경
     * - 새 문서를 생성하여 상신 또는 임시저장, resubmittedBy = 반려된 문서로 설정
     * - 양방향 연결 설정
     */
    public Long resubmitRejectedDocument(String comId, String myEmpId, Long originalDocNo, ReqDocumentDto reqDto) {

        // 1. 원본 문서 조회
        Document originalDoc = documentRepository.findById(originalDocNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 2. 본인 문서인지 확인
        if (!originalDoc.getWriter().getEmpId().equals(myEmpId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 3. 회사 일치 확인
        if (!originalDoc.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 4. 반려 상태인지 확인
        if (originalDoc.getDocStat() != DocStat.RJ) {
            throw new CustomException(ErrorCode.DOCUMENT_NOT_REJECTED);
        }

        // 5. 이미 재상신된 문서인지 확인
        if (Boolean.TRUE.equals(originalDoc.getIsResubmitted())) {
            throw new CustomException(ErrorCode.DOCUMENT_ALREADY_RESUBMITTED);
        }

        // 6. 연관 엔티티 조회
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Employee writer = employeeRepository.findByEmpId(myEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        DocumentFormCategory category = tempDocumentFormCategoryRepository.findById(reqDto.getDocfoCatNo())
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_CATEGORY_NOT_FOUND));

        DocumentForm form = tempDocumentFormRepository.findById(reqDto.getDocfoNo())
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        // 7. 새 문서 생성
        Document newDocument = Document.toEntity(reqDto, company, writer, category, form);

        boolean isTemp = Boolean.TRUE.equals(reqDto.getTemp());

        if (isTemp) {
            newDocument.saveAsTemp();
        } else {
            newDocument.submit();
        }

        documentRepository.save(newDocument);

        // 8. 결재라인 생성
        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()) {
            validateApproverNotSelf(myEmpId, reqDto.getApprovalLines());
            validateApprovalLineOrder(reqDto.getApprovalLines());
            createApprovalLines(newDocument, company, reqDto.getApprovalLines(), isTemp);
        }

        // 9. 양방향 연결 설정 (핵심!)
        originalDoc.linkResubmission(newDocument);

        log.info("반려 문서 재작성 완료: originalDocNo={}, newDocNo={}, isTemp={}",
                originalDocNo, newDocument.getDocNo(), isTemp);

        return newDocument.getDocNo();
    }


    /**
     * 임시저장 문서 수정 (UPDATE)
     */
    public void updateTempDocument(String comId, String myEmpId, Long docNo, ReqDocumentDto reqDto) {

        // 1. 문서 조회
        Document document = documentRepository.findById(docNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 2. 본인 문서인지 확인
        if (!document.getWriter().getEmpId().equals(myEmpId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 3. 회사 일치 확인
        if (!document.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 4. 임시저장 상태인지 확인
        if (document.getDocStat() != DocStat.US) {
            throw new CustomException(ErrorCode.DOCUMENT_NOT_TEMP);
        }

        // 5. 카테고리 조회
        DocumentFormCategory category = tempDocumentFormCategoryRepository.findById(reqDto.getDocfoCatNo())
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_CATEGORY_NOT_FOUND));

        // 6. 문서 내용 수정
        document.update(reqDto.getTitle(), reqDto.getContent(), reqDto.getCnttHtml(), reqDto.getAiSumm(), category);

        boolean isTemp = Boolean.TRUE.equals(reqDto.getTemp());

        if (isTemp) {
            document.saveAsTemp();
        } else {
            document.submit();
        }

        // 7. 기존 결재라인 삭제 후 새로 생성
        approvalLineRepository.deleteByDocument_docNo(docNo);

        if (reqDto.getApprovalLines() != null && !reqDto.getApprovalLines().isEmpty()) {
            validateApproverNotSelf(myEmpId, reqDto.getApprovalLines());
            validateApprovalLineOrder(reqDto.getApprovalLines());

            Company company = document.getCompany();
            createApprovalLines(document, company, reqDto.getApprovalLines(), isTemp);
        }

        log.info("임시저장 문서 수정 완료: docNo={}, temp={}", docNo, isTemp);
    }


    /**
     * AI 요약 생성 (GPT-4o-mini 사용)
     */
    public String generateAiSummary(String content) {

        log.info("AI 요약 생성 시작 - contentLength: {}", content.length());

        try {
            ReqDocOpenAiDto request = ReqDocOpenAiDto.builder()
                    .model(documentOpenAiConfig.getModel())
                    .messages(List.of(
                            ReqDocOpenAiDto.Message.builder()
                                    .role("system")
                                    .content("당신은 기업 결재 문서를 요약하는 전문 AI입니다.\n" +
                                            "\n" +
                                            "**입력 형식:**\n" +
                                            "- HTML 형식의 문서 (표, 리스트 포함 가능)\n" +
                                            "\n" +
                                            "**요약 규칙:**\n" +
                                            "1. HTML 태그는 무시하고 내용만 파악\n" +
                                            "2. 표(table)의 경우: 각 셀의 내용을 문맥으로 이해하여 핵심만 추출\n" +
                                            "3. 리스트의 경우: 항목들을 그룹화하여 요약\n" +
                                            "4. 최종 요약은 100자 이내로 작성\n" +
                                            "5. 핵심 요청사항, 보고사항, 주요 일정만 포함\n" +
                                            "6. 명확하고 간결한 한국어 문장\n" +
                                            "\n" +
                                            "**좋은 요약 예시:**\n" +
                                            "- \"어제: 데이터 정리 및 대직자 이슈 해결. 오늘: 화면 검증 및 양식 개선. 내일: 코드리뷰 예정\"\n" +
                                            "- \"1분기 마케팅 예산 500만원에서 800만원으로 증액 요청 (경쟁사 대응)\"\n" +
                                            "\n" +
                                            "**나쁜 요약 예시:**\n" +
                                            "- \"어제한거 데이터 이쁘게 넣기 임시저장에서...\" (맥락 없음)\n" +
                                            "- \"표에 항목1, 항목2가 있고...\" (구조 설명)")
                                    .build(),
                            ReqDocOpenAiDto.Message.builder()
                                    .role("user")
                                    .content("다음 문서를 요약해주세요:\n\n" + content)
                                    .build()
                    ))
                    .maxTokens(documentOpenAiConfig.getMaxTokens())
                    .temperature(0.3)  // 낮은 temperature = 더 일관된 요약
                    .build();

            ResDocOpenAiDto response = documentOpenAiWebClient.post()
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ResDocOpenAiDto.class)
                    .block();

            if (response == null || response.getChoices().isEmpty()) {
                throw new CustomException(ErrorCode.AI_SUMMARY_GENERATION_FAILED);
            }

            String summary = response.getChoices().get(0).getMessage().getContent().trim();

            log.info("AI 요약 생성 완료 - summaryLength: {}", summary.length());

            return summary;

        } catch (Exception e) {
            log.error("AI 요약 생성 실패", e);
            throw new CustomException(ErrorCode.AI_SUMMARY_GENERATION_FAILED);
        }
    }


    /**
     * 임시저장 문서 삭제
     */
    public void deleteDocument(String comId, String myEmpId, Long docNo) {
        // 1. 문서 조회
        Document document = documentRepository.findById(docNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 2. 본인 문서인지 확인
        if (!document.getWriter().getEmpId().equals(myEmpId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 3. 회사 일치 확인
        if (!document.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // 4. 임시저장(상신 전) 상태인지 확인 (이미 상신된 문서는 삭제 불가)
        if (document.getDocStat() != DocStat.US || !Boolean.TRUE.equals(document.getTemp())) {
            throw new CustomException(ErrorCode.CANNOT_DELETE_SUBMITTED_DOCUMENT); // 에러코드 추가 필요
        }

        if (document.getResubmittedBy() != null) {
            Document originalDoc = document.getResubmittedBy();
            originalDoc.clearResubmissionLink();
            log.info("원본 문서(docNo={})의 재상신 잠금을 해제했습니다.", originalDoc.getDocNo());
        }

        // 5. 연관된 결재라인 먼저 삭제
        approvalLineRepository.deleteByDocument_docNo(docNo);

        // 6. 첨부파일이 있다면 여기서 처리 (필요 시 첨부파일 삭제 로직 호출)
        // attachmentService.deleteAttachmentsByEntity("APPROVAL", docNo);

        // 7. 문서 삭제
        documentRepository.delete(document);

        log.info("임시저장 문서 삭제 완료: docNo={}, writer={}", docNo, myEmpId);
    }


    /**
     * 결재자들에게 결재 요청 알림 전송 (작성자 이름 포함)
     */
    private void notifyApprovers(Document document, int seq) {
        String writerName = document.getWriter().getEmpName(); // 작성자 이름

        List<ApprovalLine> approvers = approvalLineRepository.findByDocument_docNo(document.getDocNo())
                .stream()
                .filter(line -> line.getSeq() == seq)
                .toList();

        for (ApprovalLine line : approvers) {
            notificationService.sendNotification(
                    line.getApprover(),
                    NotificationType.APPROVAL,
                    "[결재 요청]",
                    writerName + "님이 상신한 '" + document.getTitle() + "' 문서의 결재 차례입니다.",
                    "/documents/" + document.getDocNo() + "?status=AWAITING"
            );
        }
    }

    @Transactional(readOnly = true)
    public String determineActualStatus(String comId, String myEmpId, Long docNo, String requestedStatus) {
        Document doc = documentRepository.findByIdWithApprovalLines(comId, docNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));

        // 1. 해당 사용자가 연관된 모든 결재 라인 추출 (원 결재자 또는 대직자)
        List<ApprovalLine> myLines = doc.getApprovalLines().stream()
                .filter(al -> al.getApprover().getEmpId().equals(myEmpId) ||
                        (al.getApprover().getDelegate() != null && al.getApprover().getDelegate().getEmpId().equals(myEmpId)))
                .toList();

        // 2. 권한 집합 판별
        boolean isWriter = doc.getWriter().getEmpId().equals(myEmpId);
        boolean hasAwaitingRole = myLines.stream().anyMatch(al -> al.getApprStat() == ApprStat.I || al.getApprStat() == ApprStat.W);
        boolean hasProcessedRole = myLines.stream().anyMatch(al -> al.getApprStat() == ApprStat.A || al.getApprStat() == ApprStat.R);
        DocStat docStat = doc.getDocStat();

        // 3. [최우선] 사용자 의도 존중 (다중 역할이라도 요청한 상태가 유효하면 통과)
        if ("SUBMITTED".equals(requestedStatus) && isWriter && docStat != DocStat.US) return "SUBMITTED";
        if ("UNSUBMITTED".equals(requestedStatus) && isWriter && docStat == DocStat.US) return "UNSUBMITTED";

        // 사용자가 1번은 결재했고 3번은 대기 중일 때,
        // '결재할 문서'에서 클릭했다면 AWAITING을, '결재한 문서'에서 클릭했다면 PROCESSED를 보여줌
        if ("AWAITING".equals(requestedStatus) && hasAwaitingRole && docStat == DocStat.AW) return "AWAITING";
        if ("PROCESSED".equals(requestedStatus) && hasProcessedRole) return "PROCESSED";

        if ("FINALIZED".equals(requestedStatus) && docStat == DocStat.FI) return "FINALIZED";

        // 4. [자동 리다이렉트] 의도가 불분명할 때 우선순위 가이드
        // 현재 당장 결재해야 할 건(I, W)이 있다면 결재 페이지로 먼저 안내
        if (docStat == DocStat.AW && hasAwaitingRole) return "AWAITING";
        if (hasProcessedRole) return "PROCESSED";
        if (isWriter) return (docStat == DocStat.US) ? "UNSUBMITTED" : "SUBMITTED";

        return requestedStatus;
    }

    /**
     * 작성자에게 결재 결과 알림 전송 (결재자 이름 포함된 content를 인자로 받음)
     */
    private void notifyWriter(Document document, String title, String content) {
        notificationService.sendNotification(
                document.getWriter(),
                NotificationType.APPROVAL,
                title,
                content,
                "/documents/" + document.getDocNo() + "?status=SUBMITTED"
        );
    }
}
