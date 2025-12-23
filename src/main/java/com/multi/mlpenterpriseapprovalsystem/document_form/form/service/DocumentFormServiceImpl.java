package com.multi.mlpenterpriseapprovalsystem.document_form.form.service;

import com.multi.mlpenterpriseapprovalsystem.company.domain.*;
import com.multi.mlpenterpriseapprovalsystem.company.repository.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.req.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.repository.*;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

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
    public Page<ResDocumentFormListDto> findListByStatus(
            DocumentFormStats stat,
            Pageable pageable
    ) {
        return documentFormRepository.findListByDocfoStat(stat, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ResDocumentFormDetailDto findDetailById(Long docfoNo) {

        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "DocumentForm not found. docfoNo=" + docfoNo
                        )
                );

        List<String> categories = documentFormCategoryRepository
                .findByDocumentForm_DocfoNo(docfoNo)
                .stream()
                .map(DocumentFormCategory::getName)
                .toList();

        return new ResDocumentFormDetailDto(
                form.getDocfoNo(),
                form.getDocfoName(),
                form.getDocfoStat(),
                form.getCnttJson(),
                form.getCnttHtml(),
                categories
        );
    }

    @Override
    @Transactional
    public Long createDocumentForm(ReqDocumentFormCreateDto req) {
        Company company = companyRepository.findByComId(req.comId())
                .orElseThrow(() -> new EntityNotFoundException("Company not found. comId=" + req.comId()));
        Employee writer = employeeRepository.findByEmpId(req.writerId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found. writerId=" + req.writerId()));

        // 1) 문서양식 저장
        DocumentForm form = DocumentForm.create(
                req.comId(),
                req.writerId(),
                req.docfoName(),
                req.cnttJson(),
                req.cnttHtml()
        );
        DocumentForm saved = documentFormRepository.save(form);

        // 2) 카테고리 저장 (옵션: null/빈값 방어)
        List<String> categories = req.categories();
        if (categories != null && !categories.isEmpty()) {
            List<DocumentFormCategory> catEntities = categories.stream()
                    .filter(n -> n != null && !n.isBlank())
                    .map(String::trim)
                    .distinct()
                    .map(n -> DocumentFormCategory.create(company, saved, n))
                    .toList();
            documentFormCategoryRepository.saveAll(catEntities);
        }
        return saved.getDocfoNo();
    }

    @Override
    @Transactional
    public void deleteDocumentForm(Long docfoNo) {
        DocumentForm form = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new IllegalArgumentException("문서 양식 없음"));
        form.delete();
    }

    @Override
    @Transactional
    public Long updateDocumentForm(Long docfoNo, ReqDocumentFormCreateDto req) {

        // 1) 기존 양식 조회
        DocumentForm oldForm = documentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new IllegalArgumentException("문서 양식 없음"));

        //2) 삭제 가능한 문서인지 조회
        if(oldForm.getDocfoStat()==DocumentFormStats.D){
            throw new RuntimeException("이미 삭제된 문서입니다.");
        }

        // 3) 기존 양식 상태 D로 변경
        oldForm.delete();

        // 4) 새 양식 생성 & 저장
        DocumentForm newForm = DocumentForm.create(
                oldForm.getComId(),
                req.writerId(),
                req.docfoName(),
                req.cnttJson(),
                req.cnttHtml()
        );

        DocumentForm saved = documentFormRepository.save(newForm);

        return saved.getDocfoNo();
    }
}