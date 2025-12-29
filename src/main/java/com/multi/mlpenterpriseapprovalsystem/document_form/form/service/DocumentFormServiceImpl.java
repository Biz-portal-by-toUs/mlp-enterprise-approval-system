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

    // 목록 조회 (+ 제목 검색 옵션)
    @Override
    @Transactional(readOnly = true)
    public Page<ResDocumentFormListDto> findListByStatus(
            DocumentFormStats stat,
            String comId,
            String keyword,
            Pageable pageable
    ) {
        boolean hasKeyword = (keyword != null && !keyword.isBlank());

        Page<DocumentForm> page = hasKeyword
                ? documentFormRepository
                .findByDocfoStatAndCompany_ComIdAndDocfoNameContainingIgnoreCaseOrderByDocfoNoAsc(
                        stat, comId, keyword.trim(), pageable
                )
                : documentFormRepository
                .findByDocfoStatAndCompany_ComIdOrderByDocfoNoAsc(stat, comId, pageable);

        return page.map(f -> new ResDocumentFormListDto(
                f.getDocfoNo(),
                f.getCompany(),
                f.getWriter(),
                f.getDocfoName(),
                f.getDocfoStat(),
                f.getRejectReason()
        ));
    }

    // 상세 조회
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
                categories
        );
    }

    // 생성
    @Override
    @Transactional
    public Long createDocumentForm(
            ReqDocumentFormCreateDto req,
            String comId,
            String writerId
    ) {
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
        ); // create() 안에서 P로 설정됨

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

    // 수정
    @Override
    @Transactional
    public Long updateDocumentForm(
            Long docfoNo,
            ReqDocumentFormCreateDto req,
            String comId,
            String empId
    ) {
        DocumentForm origin = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new IllegalArgumentException("문서 양식 없음"));

        validateCompany(origin, comId);

        // 기존 문서 삭제 처리
        documentFormRepository.updateDocfoStat(origin.getDocfoNo(), DocumentFormStats.D);

        Employee writer = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보 없음"));

        DocumentForm newForm = DocumentForm.create(
                origin.getCompany(),
                writer,
                req.docfoName(),
                ensureJsonString(req.cnttJson()),
                req.cnttHtml()
        );

        documentFormRepository.save(newForm);

        if (req.categories() != null && !req.categories().isEmpty()) {
            List<DocumentFormCategory> categories =
                    req.categories().stream()
                            .map(name -> DocumentFormCategory.create(
                                    origin.getCompany(),
                                    newForm,
                                    name
                            ))
                            .toList();

            documentFormCategoryRepository.saveAll(categories);
        }

        return newForm.getDocfoNo();
    }

    // 삭제
    @Override
    @Transactional
    public void deleteDocumentForm(Long docfoNo, String comId) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found"));

        validateCompany(form, comId);

        documentFormRepository.updateDocfoStat(docfoNo, DocumentFormStats.D);
    }

    // 승인 / 반려
    @Override
    @Transactional
    public void changeStatus(
            Long docfoNo,
            String comId,
            DocumentFormStats stat,
            String rejectReason
    ) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found"));

        validateCompany(form, comId);

        if (stat != DocumentFormStats.A && stat != DocumentFormStats.R) {
            throw new IllegalArgumentException("허용되지 않은 상태 변경");
        }

        documentFormRepository.updateDocfoStat(docfoNo, stat);

        if (stat == DocumentFormStats.R) {
            if (rejectReason == null || rejectReason.isBlank()) {
                throw new IllegalArgumentException("반려 사유는 필수입니다.");
            }
            documentFormRepository.updateRejectReason(docfoNo, rejectReason);
        } else {
            // 승인 시 반려 사유 제거
            documentFormRepository.updateRejectReason(docfoNo, null);
        }
    }

    //공통 함수
    private String ensureJsonString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("cnttJson is empty");
        }

        String s = raw.trim();

        if (s.startsWith("<")) {
            throw new IllegalArgumentException("cnttJson must be JSON");
        }

        boolean isObj = s.startsWith("{") && s.endsWith("}");
        boolean isArr = s.startsWith("[") && s.endsWith("]");

        if (!isObj && !isArr) {
            throw new IllegalArgumentException("cnttJson must be JSON string");
        }

        return s;
    }

    private void validateCompany(DocumentForm form, String comId) {
        if (!form.getCompany().getComId().equals(comId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }
}