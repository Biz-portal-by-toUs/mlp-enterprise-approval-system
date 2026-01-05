package com.multi.mlpenterpriseapprovalsystem.document_form.form.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.company.domain.*;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentFormCategory;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.repository.DocumentFormCategoryRepository;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.repository.DocumentFormRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Please explain the class!!!
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

    @Override
    @Transactional(readOnly = true)
    public Page<ResDocumentFormListDto> findListByStatuses(
            List<DocumentFormStats> stats, String comId, String keyword, Pageable pageable
    ) {
        // 비어있으면 기본값 A (안전장치)
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
                .orElseThrow(() -> new EntityNotFoundException("문서 양식을 찾을 수 없습니다."));

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

    @Override
    @Transactional
    public Long createDocumentForm(ReqDocumentFormCreateDto req, String comId, String writerId) {

        validateDocfoNameForbidden(req.docfoName());

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new EntityNotFoundException("회사 없음"));

        Employee writer = employeeRepository.findByEmpId(writerId)
                .orElseThrow(() -> new EntityNotFoundException("작성자 없음"));

        DocumentForm form = DocumentForm.create(
                company,
                writer,
                req.docfoName(),
                ensureJsonString(req.cnttJson()),
                null
        );

        DocumentForm saved = documentFormRepository.save(form);

        if (req.categories() != null && !req.categories().isEmpty()) {
            List<DocumentFormCategory> categories =
                    req.categories().stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .map(name -> DocumentFormCategory.create(company, saved, name))
                            .toList();

            documentFormCategoryRepository.saveAll(categories);
        }

        return saved.getDocfoNo();
    }

    @Override
    @Transactional
    public Long updateDocumentForm(Long docfoNo, ReqDocumentFormCreateDto req, String comId, String empId) {

        validateDocfoNameForbidden(req.docfoName());

        DocumentForm origin = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new IllegalArgumentException("문서 양식 없음"));

        validateCompany(origin, comId);

        // 기존 문서 삭제 처리(논리삭제)
        documentFormRepository.updateStatusAndReason(origin.getDocfoNo(), DocumentFormStats.D, null);

        Employee writer = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보 없음"));

        DocumentForm newForm = DocumentForm.create(
                origin.getCompany(),
                writer,
                req.docfoName(),
                ensureJsonString(req.cnttJson()),
                req.cnttHtml()
        );

        DocumentForm saved = documentFormRepository.save(newForm);

        if (req.categories() != null && !req.categories().isEmpty()) {
            List<DocumentFormCategory> categories =
                    req.categories().stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .map(name -> DocumentFormCategory.create(origin.getCompany(), saved, name))
                            .toList();

            documentFormCategoryRepository.saveAll(categories);
        }

        return saved.getDocfoNo();
    }

    // 승인/반려(A/R) 전용
    @Override
    @Transactional
    public void changeApproveOrReject(Long docfoNo, String comId, DocumentFormStats next, String rejectReason) {
        if (next != DocumentFormStats.A && next != DocumentFormStats.R) {
            throw new IllegalArgumentException("A(승인) 또는 R(반려)만 가능합니다.");
        }

        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found"));

        validateCompany(form, comId);

        DocumentFormStats cur = form.getDocfoStat();

        if (cur == DocumentFormStats.D || cur == DocumentFormStats.W || cur == DocumentFormStats.X) {
            throw new IllegalStateException("삭제 흐름 상태에서는 승인/반려를 변경할 수 없습니다.");
        }

        // R/X가 아니면 null로 만드는 규칙 강제
        String normalized = normalizeRejectReason(next, rejectReason);

        documentFormRepository.updateStatusAndReason(docfoNo, next, normalized);
    }
    // 삭제 플로우
    @Override
    @Transactional
    public void requestDelete(Long docfoNo, String comId, CustomUser requester) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found"));

        validateCompany(form, comId);

        // 이미 삭제/삭제대기면 불가
        if (form.getDocfoStat() == DocumentFormStats.D) throw new IllegalStateException("이미 삭제된 양식입니다.");
        if (form.getDocfoStat() == DocumentFormStats.W) throw new IllegalStateException("이미 삭제대기 상태입니다.");

        // 삭제요청은 관리자만 가능
        boolean isAdmin = requester.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_COM_ADMIN")
                        || a.getAuthority().equals("ROLE_SEC_ADMIN")
                        || a.getAuthority().equals("ROLE_THR_ADMIN")
        );
        if (!isAdmin) throw new AccessDeniedException("권한이 없습니다.");

        // W로 바꿀 때 사유는 무조건 null
        documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.W, null);
    }

    @Override
    @Transactional
    public void approveDelete(Long docfoNo, String comId) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found"));

        validateCompany(form, comId);

        if (form.getDocfoStat() != DocumentFormStats.W) {
            throw new IllegalStateException("삭제대기(W) 상태에서만 삭제 승인할 수 있습니다.");
        }

        // D로 바꿀 때 사유는 무조건 null
        documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.D, null);
    }

    @Override
    @Transactional
    public void rejectDelete(Long docfoNo, String comId, String rejectReason) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found"));

        validateCompany(form, comId);

        if (form.getDocfoStat() != DocumentFormStats.W) {
            throw new IllegalStateException("삭제대기(W) 상태에서만 삭제 반려할 수 있습니다.");
        }

        // X는 사유 필수 + trim
        String normalized = normalizeRejectReason(DocumentFormStats.X, rejectReason);

        documentFormRepository.updateStatusAndReason(docfoNo, DocumentFormStats.X, normalized);
    }

    @Override
    @Transactional
    public Long createTemp(ReqDocumentFormTempDto req, String comId, String writerId) {

        validateDocfoNameForbidden(req.docfoName());

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new EntityNotFoundException("회사 없음"));

        Employee writer = employeeRepository.findByEmpId(writerId)
                .orElseThrow(() -> new EntityNotFoundException("작성자 없음"));

        DocumentForm form = DocumentForm.create(
                company,
                writer,
                req.docfoName(),
                ensureJsonString(req.cnttJson()),
                req.cnttHtml()
        );

        // 임시저장 상태 강제
        DocumentForm saved = documentFormRepository.save(form);

        // 카테고리도 저장한다고 했으니 그대로
        if (req.categories() != null && !req.categories().isEmpty()) {
            List<DocumentFormCategory> categories =
                    req.categories().stream()
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .map(name -> DocumentFormCategory.create(company, saved, name))
                            .toList();

            documentFormCategoryRepository.saveAll(categories);
        }

        // 상태를 확실히 T로 맞추고 싶으면(도메인에서 T 보장 못할 때)
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
                .orElseThrow(() -> new EntityNotFoundException("문서 양식 없음"));

        // 회사 검증
        validateCompany(form, comId);

        // "내가 작성중이던 것만" 이 규칙이면 작성자 검증도 걸어줘야 함
        if (!form.getWriter().getEmpId().equals(writerId)) {
            throw new AccessDeniedException("본인이 작성한 임시양식만 수정할 수 있습니다.");
        }

        // 삭제 흐름/승인 흐름 막기 (임시저장은 T만 덮어쓰게 하는게 안전)
        if (form.getDocfoStat() != DocumentFormStats.T) {
            throw new IllegalStateException("임시저장(T) 상태에서만 임시저장 덮어쓰기가 가능합니다.");
        }

        documentFormRepository.updateDraft(
                docfoNo,
                req.docfoName().trim(),
                ensureJsonString(req.cnttJson()),
                req.cnttHtml(),
                DocumentFormStats.T
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResDocumentFormListDto> findMyTempList(
            String comId,
            String writerId,
            String keyword,
            Pageable pageable
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

    // 공통
    private String ensureJsonString(String raw) {
        if (raw == null || raw.trim().isEmpty()) throw new IllegalArgumentException("cnttJson is empty");
        String s = raw.trim();
        if (s.startsWith("<")) throw new IllegalArgumentException("cnttJson must be JSON");

        boolean isObj = s.startsWith("{") && s.endsWith("}");
        boolean isArr = s.startsWith("[") && s.endsWith("]");
        if (!isObj && !isArr) throw new IllegalArgumentException("cnttJson must be JSON string");

        return s;
    }

    private void validateCompany(DocumentForm form, String comId) {
        if (!form.getCompany().getComId().equals(comId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    // R/X면 사유 필수, 그 외는 무조건 null로 정규화
    private String normalizeRejectReason(DocumentFormStats next, String reason) {
        if (next == DocumentFormStats.R || next == DocumentFormStats.X) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("반려 사유는 필수입니다.");
            }
            return reason.trim();
        }
        return null;
    }

    // 양식 제목 금칙어
    private void validateDocfoNameForbidden(String docfoName) {
        String name = (docfoName == null) ? "" : docfoName.trim();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("문서 양식 제목은 필수입니다.");
        }

        // 포함되면 차단할 키워드들
        List<String> forbidden = List.of("휴가", "출장");

        for (String w : forbidden) {
            if (name.contains(w)) {
                throw new IllegalArgumentException("문서 양식 제목에 금칙어가 포함되어 생성/수정할 수 없습니다: " + w);
            }
        }
    }

    // 임시저장 덮어쓰기 허용 상태 검증 (추천: T/R만 허용)
    private void validateTempUpdatableStatus(DocumentFormStats cur) {
        if (cur == DocumentFormStats.T || cur == DocumentFormStats.R) return;

        // 필요하면 P에서도 임시 덮어쓰기 허용 가능:
        // if (cur == DocumentFormStats.P) return;

        throw new IllegalStateException("임시저장은 T(임시) 또는 R(반려) 상태에서만 가능합니다.");
    }

    /**
     * 임시저장 제목 정책
     * - incoming이 비면 origin 유지
     * - origin도 없으면 "임시 문서"
     */
    private String normalizeTempDocfoName(String incoming, String origin) {
        if (incoming == null || incoming.trim().isEmpty()) {
            if (origin == null || origin.isBlank()) return "임시 문서";
            return origin;
        }
        return incoming.trim();
    }

    /**
     * 임시저장 JSON 정책
     * - null/blank면 기본 JSON("{}") 주입
     * - 값이 있으면 최소 검증(HTML 방지 + JSON 형태 체크)
     */
    private String normalizeTempJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "{}";

        String s = raw.trim();
        if (s.startsWith("<")) throw new IllegalArgumentException("cnttJson must be JSON");

        boolean isObj = s.startsWith("{") && s.endsWith("}");
        boolean isArr = s.startsWith("[") && s.endsWith("]");
        if (!isObj && !isArr) throw new IllegalArgumentException("cnttJson must be JSON string");

        return s;
    }
}