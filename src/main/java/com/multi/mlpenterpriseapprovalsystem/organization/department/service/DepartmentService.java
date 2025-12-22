package com.multi.mlpenterpriseapprovalsystem.organization.department.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.department.dto.ReqDepartmentAddDto;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부서 등록, 수정, 삭제, 조회 서비스
 *
 * @author : 권지영
 * @filename : DepartmentService
 * @since : 2025. 12. 22. 월요일
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;

    public void addDepartment(String comId, @Valid ReqDepartmentAddDto reqDepartmentAddDto) {

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 부서 코드가 이미 존재하는 경우
        if(departmentRepository.existsByCompanyAndDepId(company, reqDepartmentAddDto.getDepId())) {

            throw new CustomException(ErrorCode.DUPLICATE_DEPID);
        }

        // 부서 이름이 이미 존재하는 경우
        if(departmentRepository.existsByCompanyAndDepName(company, reqDepartmentAddDto.getDepName())) {

            throw new CustomException(ErrorCode.DUPLICATE_DEPNAME);
        }

        Department department = Department.of(company, reqDepartmentAddDto.getDepId().trim(), reqDepartmentAddDto.getDepName().trim());
        departmentRepository.save(department);
    }

}
