package com.multi.mlpenterpriseapprovalsystem.documentform.form.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentFormCategory;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.req.ReqDocumentFormCreateDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.req.ReqDocumentFormTempDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res.ResDocumentFormCategoryNameDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res.ResDocumentFormDetailDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.dto.res.ResDocumentFormListDto;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.repository.DocumentFormCategoryRepository;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.repository.DocumentFormRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 문서양식 서비스(함수 실행부)
 *
 * @author : 정종원
 * @filename : DocumentFormServiceImpl
 * @since : 2025-12-22 월요일
 */
@Service
@RequiredArgsConstructor
public class DocumentFormServiceImpl implements DocumentFormService {

    private final DocumentFormRepository documentFormRepository;
    private final DocumentFormCategoryRepository documentFormCategoryRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    // 조회
    @Override
    @Transactional(readOnly = true)
    public Page<ResDocumentFormListDto> findListByStatuses(
            List<DocumentFormStats> stats, String comId, String keyword, Pageable pageable
    ) {
        List<DocumentFormStats> safeStats =
                (stats == null || stats.isEmpty()) ? List.of(DocumentFormStats.A) : stats;

        boolean hasKeyword = (keyword != null && !keyword.isBlank());
        String kw = hasKeyword ? keyword.trim() : null;

        Page<DocumentForm> page = hasKeyword
                ? documentFormRepository.findByDocfoStatInAndCompany_ComIdAndDocfoNameContainingIgnoreCaseOrderByDocfoNoAsc(
                safeStats, comId, kw, pageable
        )
                : documentFormRepository.findByDocfoStatInAndCompany_ComIdOrderByDocfoNoAsc(
                safeStats, comId, pageable
        );

        return page.map(f -> new ResDocumentFormListDto(
                f.getDocfoNo(),
                f.getCompany(),
                f.getWriter(),
                f.getDocfoName(),
                f.getDocfoStat(),
                f.getRejectReason()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public ResDocumentFormDetailDto findDetailById(Long docfoNo, String comId) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(form, comId);

        List<ResDocumentFormCategoryNameDto> categories =
                documentFormCategoryRepository
                        .findByDocumentForm_DocfoNoOrderByDocfoCatNoAsc(docfoNo)
                        .stream()
                        .map(c -> new ResDocumentFormCategoryNameDto(c.getName()))
                        .toList();

        return new ResDocumentFormDetailDto(
                form.getDocfoNo(),
                form.getDocfoName(),
                form.getCnttJson(),
                form.getCnttHtml(),
                form.getRejectReason(),
                form.getDocfoStat(),
                categories
        );
    }

    // 생성 / 수정
    @Override
    @Transactional
    public Long createDocumentForm(ReqDocumentFormCreateDto req, String comId, String writerId) {
        validateDocfoNameForbidden(req.docfoName());

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Employee writer = employeeRepository.findByEmpId(writerId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        DocumentForm form = DocumentForm.create(
                company,
                writer,
                req.docfoName(),
                ensureJsonString(req.cnttJson()),
                null
        );

        DocumentForm saved = documentFormRepository.save(form);
        saveCategoriesIfPresent(req.categories(), company, saved);

        return saved.getDocfoNo();
    }

    /**
     * 수정 정책
     * - A(승인): 수정본을 새로 생성(originDocfoNo=원본 docfoNo), 원본은 유지
     * - T/R/P: 자기 자신을 updateDraft 로 갱신 (상태는 그대로 유지)
     * - W/X/D: 수정 불가
     */
    @Override
    @Transactional
    public Long updateDocumentForm(Long docfoNo, ReqDocumentFormCreateDto req, String comId, String empId) {
        validateDocfoNameForbidden(req.docfoName());

        DocumentForm target = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(target, comId);

        DocumentFormStats cur = target.getDocfoStat();

        // 수정 불가
        if (cur == DocumentFormStats.D || cur == DocumentFormStats.W || cur == DocumentFormStats.P) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_INVALID_NEXT_STATUS);
        }

        // A/X 편집: 복사본 생성 -> P
        if (cur == DocumentFormStats.A || cur == DocumentFormStats.X) {

            // 진행중 수정본(P) 1개만 허용 (원하면 R 포함 등으로 확장)
            boolean existsPendingCopy = documentFormRepository
                    .existsByCompany_ComIdAndOriginDocfoNoAndDocfoStatIn(
                            comId,
                            target.getDocfoNo(),
                            List.of(DocumentFormStats.P)
                    );

            if (existsPendingCopy) {
                throw new CustomException(ErrorCode.DOCUMENT_FORM_EDIT_ALREADY_EXISTS);
            }

            Employee writer = employeeRepository.findByEmpId(empId)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            DocumentForm copied = DocumentForm.create(
                    target.getCompany(),
                    writer,
                    req.docfoName(),
                    ensureJsonString(req.cnttJson()),
                    req.cnttHtml()
            );

            copied.setOriginDocfoNo(target.getDocfoNo());
            copied.setDocfoStat(DocumentFormStats.P);

            DocumentForm saved = documentFormRepository.save(copied);

            // 카테고리(복사본 기준)
            saveCategoriesIfPresent(req.categories(), target.getCompany(), saved);

            return saved.getDocfoNo();
        }

        if (cur == DocumentFormStats.T || cur == DocumentFormStats.R) {
            documentFormRepository.updateDraft(
                    docfoNo,
                    req.docfoName().trim(),
                    ensureJsonString(req.cnttJson()),
                    req.cnttHtml(),
                    DocumentFormStats.P
            );

            documentFormCategoryRepository.deleteByDocfoNo(docfoNo);
            saveCategoriesIfPresent(req.categories(), target.getCompany(), target);

            return docfoNo;
        }
        throw new CustomException(ErrorCode.DOCUMENT_FORM_INVALID_NEXT_STATUS);
    }

    // 상태 변경 (승인/반려)
    @Override
    @Transactional
    public void changeApproveOrReject(Long docfoNo, String comId, DocumentFormStats next, String rejectReason) {
        if (next != DocumentFormStats.A && next != DocumentFormStats.R) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_INVALID_NEXT_STATUS);
        }

        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(form, comId);

        DocumentFormStats cur = form.getDocfoStat();

        // 승인/반려는 P에서만 처리
        if (cur != DocumentFormStats.P) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_INVALID_NEXT_STATUS);
        }

        // 승인(A)
        if (next == DocumentFormStats.A) {
            // 수정본이면 원본을 D로 내림
            if (form.getOriginDocfoNo() != null) {
                Long originDocfoNo = form.getOriginDocfoNo();

                // 원본이 A 또는 X 인 경우에만 D로
                int updated = documentFormRepository.updateStatusForOrigin(
                        originDocfoNo,
                        comId,
                        DocumentFormStats.A,
                        DocumentFormStats.D
                );

                if (updated == 0) {
                    updated = documentFormRepository.updateStatusForOrigin(
                            originDocfoNo,
                            comId,
                            DocumentFormStats.X,
                            DocumentFormStats.D
                    );
                }

                if (updated == 0) {
                    throw new CustomException(ErrorCode.DOCUMENT_FORM_INVALID_NEXT_STATUS);
                }
            }

            // 현재 문서는 A로
            documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.A, null);
            return;
        }

        // 반려(R): 원본은 건드리지 않고 현재만 R
        String normalized = normalizeRejectReason(DocumentFormStats.R, rejectReason);
        documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.R, normalized);
    }

    // 삭제 플로우
    @Override
    @Transactional(readOnly = true)
    public void assertNotTemp(Long docfoNo, String comId) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));
        validateCompany(form, comId);

        if (form.getDocfoStat() == DocumentFormStats.T) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TEMP_ONLY_DELETE_ENDPOINT);
        }
    }

    @Override
    @Transactional
    public void requestDelete(Long docfoNo, String comId, CustomUser requester) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(form, comId);

        if (form.getDocfoStat() == DocumentFormStats.D) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_DELETE_ALREADY_DELETED);
        }
        if (form.getDocfoStat() == DocumentFormStats.W) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_DELETE_ALREADY_WAITING);
        }

        boolean isAdmin = requester.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_COM_ADMIN")
                        || a.getAuthority().equals("ROLE_SEC_ADMIN")
                        || a.getAuthority().equals("ROLE_THR_ADMIN")
        );
        if (!isAdmin) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_DELETE_REQUEST_FORBIDDEN);
        }

        documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.W, null);
    }

    @Override
    @Transactional
    public void approveDelete(Long docfoNo, String comId) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(form, comId);

        if (form.getDocfoStat() != DocumentFormStats.W) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_DELETE_ONLY_WAITING);
        }

        documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.D, null);
    }

    @Override
    @Transactional
    public void rejectDelete(Long docfoNo, String comId, String rejectReason) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(form, comId);

        if (form.getDocfoStat() != DocumentFormStats.W) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_DELETE_ONLY_WAITING);
        }

        String normalized = normalizeRejectReason(DocumentFormStats.X, rejectReason);
        documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.X, normalized);
    }

    // 임시저장
    @Override
    @Transactional
    public Long createTemp(ReqDocumentFormTempDto req, String comId, String writerId) {
        validateDocfoNameForbidden(req.docfoName());

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Employee writer = employeeRepository.findByEmpId(writerId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        DocumentForm form = DocumentForm.create(
                company,
                writer,
                req.docfoName(),
                ensureJsonString(req.cnttJson()),
                req.cnttHtml()
        );

        DocumentForm saved = documentFormRepository.save(form);

        saveCategoriesIfPresent(req.categories(), company, saved);

        // 임시저장 상태로 전환
        documentFormRepository.updateDraft(
                saved.getDocfoNo(),
                saved.getDocfoName(),
                saved.getCnttJson(),
                saved.getCnttHtml(),
                DocumentFormStats.T
        );

        return saved.getDocfoNo();
    }

    @Override
    @Transactional
    public void saveTemp(Long docfoNo, ReqDocumentFormTempDto req, String comId, String writerId) {
        validateDocfoNameForbidden(req.docfoName());

        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(form, comId);

        if (!form.getWriter().getEmpId().equals(writerId)) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TEMP_ONLY_OWNER);
        }

        if (form.getDocfoStat() != DocumentFormStats.T) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TEMP_ONLY_T);
        }

        documentFormRepository.updateDraft(
                docfoNo,
                req.docfoName().trim(),
                ensureJsonString(req.cnttJson()),
                req.cnttHtml(),
                DocumentFormStats.T
        );

        // temp도 카테고리 갱신 정책 동일하게 적용하고 싶으면 아래 주석 해제
        // documentFormCategoryRepository.deleteByDocfoNo(docfoNo);
        // saveCategoriesIfPresent(req.categories(), form.getCompany(), form);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResDocumentFormListDto> findMyTempList(
            String comId, String writerId, String keyword, Pageable pageable
    ) {
        Page<DocumentForm> page;

        if (keyword != null && !keyword.isBlank()) {
            page = documentFormRepository.searchMyByStat(
                    comId, writerId, DocumentFormStats.T, keyword.trim(), pageable
            );
        } else {
            page = documentFormRepository.findMyByStat(
                    comId, writerId, DocumentFormStats.T, pageable
            );
        }

        return page.map(f -> new ResDocumentFormListDto(
                f.getDocfoNo(),
                f.getCompany(),
                f.getWriter(),
                f.getDocfoName(),
                f.getDocfoStat(),
                f.getRejectReason()
        ));
    }

    @Override
    @Transactional
    public void deleteTemp(Long docfoNo, String comId, String writerId) {
        if (docfoNo == null || docfoNo <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        validateCompany(form, comId);

        if (!form.getWriter().getEmpId().equals(writerId)) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TEMP_ONLY_OWNER);
        }

        if (form.getDocfoStat() != DocumentFormStats.T) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TEMP_ONLY_T);
        }

        documentFormCategoryRepository.deleteByDocfoNo(docfoNo);

        int deleted = documentFormRepository.deleteMyTempById(
                docfoNo, comId, writerId, DocumentFormStats.T
        );

        if (deleted == 0) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TEMP_DELETE_TARGET_NOT_FOUND);
        }
    }

    // 공통 유틸/헬퍼
    private void saveCategoriesIfPresent(List<String> categoryNames, Company company, DocumentForm saved) {
        if (categoryNames == null || categoryNames.isEmpty()) return;

        List<DocumentFormCategory> categories =
                categoryNames.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .distinct()
                        .map(name -> DocumentFormCategory.create(company, saved, name))
                        .toList();

        documentFormCategoryRepository.saveAll(categories);
    }

    private String ensureJsonString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_CNTT_JSON_EMPTY);
        }

        String s = raw.trim();

        if (s.startsWith("<")) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_CNTT_JSON_MUST_BE_JSON);
        }

        boolean isObj = s.startsWith("{") && s.endsWith("}");
        boolean isArr = s.startsWith("[") && s.endsWith("]");
        if (!isObj && !isArr) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_CNTT_JSON_MUST_BE_JSON);
        }

        return s;
    }

    private void validateCompany(DocumentForm form, String comId) {
        if (!form.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_COMPANY_MISMATCH);
        }
    }

    private String normalizeRejectReason(DocumentFormStats next, String reason) {
        if (next == DocumentFormStats.R || next == DocumentFormStats.X) {
            if (reason == null || reason.isBlank()) {
                throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED_FOR_REJECT);
            }
            return reason.trim();
        }
        return null;
    }

    private void validateDocfoNameForbidden(String docfoName) {
        String name = (docfoName == null) ? "" : docfoName.trim();

        if (name.isEmpty()) {
            throw new CustomException(ErrorCode.DOCUMENT_FORM_TITLE_REQUIRED);
        }

        List<String> forbidden = List.of("휴가", "출장");
        for (String w : forbidden) {
            if (name.contains(w)) {
                throw new CustomException(ErrorCode.DOCUMENT_FORM_TITLE_FORBIDDEN);
            }
        }
    }
}