package com.multi.mlpenterpriseapprovalsystem.document.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.*;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDepartmentRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDocumentFormCategoryRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempDocumentFormRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.TempEmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentFormCategory;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempDepartmentService
 * @since : 25. 12. 20. 토요일
 */
@Service
@Transactional
@RequiredArgsConstructor
public class TempAllService {

    private final TempDepartmentRepository tempDepartmentRepository;
    private final TempEmployeeRepository tempEmployeeRepository;
    private final TempDocumentFormRepository tempDocumentFormRepository;
    private final TempDocumentFormCategoryRepository tempDocumentFormCategoryRepository;

    // 사원번호로 사원 정보 조회
    @Transactional(readOnly = true)
    public TempResEmployeeDto getEmployeeByEmpId(String empId) {
        Employee employee = tempEmployeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        TempResEmployeeDto tempResEmployeeDto = TempResEmployeeDto.toDto(employee);
        // 부서 세팅
        tempResEmployeeDto.setDepartment(TempResDepartmentDto.toDto(employee.getDepartment()));
        // 포지션 세팅
        tempResEmployeeDto.setPosition(TempResPositionDto.toDto(employee.getPositions()));
        // 대직자 세팅
        tempResEmployeeDto.setDelegate(TempResEmployeeDto.toDto(employee.getDelegate()));

        return tempResEmployeeDto;
    }

    @Transactional(readOnly = true)
    public List<TempResDepartmentDto> getDepartments(String comId) {
        List<Department> departments = tempDepartmentRepository.findAllByCompany_comId(comId);

        return departments.stream()                // 1. 스트림 생성
                .map(TempResDepartmentDto::toDto)  // 2. DTO로 변환
                .toList();                         // 3. 리스트로 반환 (Java 16+)
    }

    // 문서 양식 내 카테고리 이름만 중복없이 조회
    @Transactional(readOnly = true)
    public List<String> getDocumentFormCategoryNames(String comId) {
        return tempDocumentFormCategoryRepository.findDistinctNamesByComId(comId);
    }


    // 문서양식 식별자로 문서양식 및 문서양식 내 카테고리 조회
    public TempResDocumentFormDto getDocumentFormWithCategory(String comId, Long docfoNo) {
        DocumentForm documentForm = tempDocumentFormRepository.findById(docfoNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_FORM_NOT_FOUND));

        List<DocumentFormCategory> documentFormCategories = tempDocumentFormCategoryRepository.findAllByDocumentForm_DocfoNo(docfoNo);

        TempResDocumentFormDto tempResDocumentFormDto = TempResDocumentFormDto.toDto(documentForm);
        List<TempResDocumentFormCategoryDto> tempResDocumentFormCategoryDtos = documentFormCategories.stream()
                .map(TempResDocumentFormCategoryDto::toDto)
                .toList();

        tempResDocumentFormDto.setTempResDocumentFormCategoryDtos(tempResDocumentFormCategoryDtos);

        return tempResDocumentFormDto;
    }

    // 전체 문서 양식 조회
    public List<TempResDocumentFormDto> getAllDocumentForms(String comId) {
        List<DocumentForm> documentForms = tempDocumentFormRepository.findAllByCompany_ComIdAndDocfoStat(comId, DocumentFormStats.A);

        List<TempResDocumentFormDto> tempResDocumentFormDtos = documentForms.stream()
                .map(TempResDocumentFormDto::toDto)
                .toList();

        return tempResDocumentFormDtos;
    }
}
