package com.multi.mlpenterpriseapprovalsystem.document_form.form.service;

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
@Transactional
public class DocumentFormServiceImpl implements DocumentFormService {

    private final DocumentFormRepository documentFormRepository;
    private final DocumentFormCategoryRepository documentFormCategoryRepository;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ResDocumentFormListDto> findListByStatus(DocumentFormStats stat, String comId, Pageable pageable) {
        return documentFormRepository
                .findByDocfoStatAndCompany_ComIdOrderByDocfoNoAsc(stat, comId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ResDocumentFormDetailDto findDetailById(Long docfoNo, String comId) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow();

        if (!form.getCompany().getComId().equals(comId)) {
            throw new AccessDeniedException("권한 없음");
        }

        List<ResDocumentFormCategoryNameDto> categories =
                documentFormCategoryRepository.findByDocumentForm_DocfoNo(docfoNo).stream()
                        .map(c -> new ResDocumentFormCategoryNameDto(c.getName()))
                        .toList();

        return new ResDocumentFormDetailDto(
                form.getDocfoNo(),
                form.getDocfoName(),
                form.getCnttJson(),
                form.getCnttHtml(),
                categories
        );
    }

    @Override
    public Long createDocumentForm(ReqDocumentFormCreateDto req, String comId, String writerId) {
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new EntityNotFoundException("회사를 찾을 수 없습니다. comId=" + comId));
        Employee employee = employeeRepository.findByEmpId(writerId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다. writerId=" + writerId));
        List<DocumentFormCategory> categories = new ArrayList<>();
        DocumentForm form = DocumentForm.create(
                company,
                employee,
                req.docfoName(),
                req.cnttJson(),
                req.cnttHtml()
        );
        DocumentForm saved = documentFormRepository.save(form);
        if (req.categories() != null) {
            categories =
                    req.categories().stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .map(name ->
                            DocumentFormCategory.create(
                                    company,
                                    saved,
                                    name
                            )
                    )
                    .toList();

            documentFormCategoryRepository.saveAll(categories);
        }
        return saved.getDocfoNo();
    }

    @Override
    public Long updateDocumentForm(Long docfoNo, ReqDocumentFormCreateDto req, String comId, String writerId) {
        DocumentForm old = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found: " + docfoNo));

        validateCompany(old, comId);
        documentFormRepository.updateDocfoStat(old.getDocfoNo(), DocumentFormStats.D);

        DocumentForm form = DocumentForm.create(
                old.getCompany(),
                old.getWriter(),
                req.docfoName(),
                req.cnttJson(),
                req.cnttHtml()
        );
        DocumentForm created = documentFormRepository.save(form);
        return created.getDocfoNo();
    }

    @Override
    public void deleteDocumentForm(Long docfoNo, String comId) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new EntityNotFoundException("DocumentForm not found: " + docfoNo));

        validateCompany(form, comId);

        documentFormRepository.updateDocfoStat(docfoNo, DocumentFormStats.D);
    }

    private void validateCompany(DocumentForm form, String comId) {
        if (form.getCompany() == null || form.getCompany().getComId() == null) {
            throw new IllegalStateException("DocumentForm.company is null");
        }
        if (!form.getCompany().getComId().equals(comId)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }
}