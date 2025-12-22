package com.multi.mlpenterpriseapprovalsystem.organization.department.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.department.dto.ReqDepartmentDto;
import com.multi.mlpenterpriseapprovalsystem.organization.department.dto.ResDepartmentDto;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final EmployeeRepository employeeRepository;


    public void addDepartment(String comId, @Valid ReqDepartmentDto reqDepartmentDto) {

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 부서 코드가 이미 존재하는 경우
        if(departmentRepository.existsByCompanyAndDepId(company, reqDepartmentDto.getDepId())) {

            throw new CustomException(ErrorCode.DUPLICATE_DEPID);
        }

        // 부서 이름이 이미 존재하는 경우
        if(departmentRepository.existsByCompanyAndDepName(company, reqDepartmentDto.getDepName())) {

            throw new CustomException(ErrorCode.DUPLICATE_DEPNAME);
        }

        Department department = Department.of(company, reqDepartmentDto.getDepId().trim(), reqDepartmentDto.getDepName().trim());
        departmentRepository.save(department);
    }

    public List<ResDepartmentDto> getDepartmentsWithEmpCount(String comId) {

        // 1) 부서 목록
        List<Department> departments = departmentRepository.findAllByCompany_ComId(comId);
        if (departments.isEmpty()) {
            return List.of(); // 2번 쿼리 안 탐
        }

        // 2) 부서별 사원수 (한 방 쿼리)
        Map<String, Long> countMap = employeeRepository.countGroupByDepId(comId).stream()
                .collect(Collectors.toMap(EmployeeRepository.DepCount::getDepId,
                        EmployeeRepository.DepCount::getCnt));

        // 3) DTO 조립
        return departments.stream()
                .map(d -> ResDepartmentDto.builder()
                        .depId(d.getDepId())
                        .depName(d.getDepName())
                        .depNo(d.getDepNo())
                        .empCount(countMap.getOrDefault(d.getDepId(), 0L))
                        .build())
                .toList();
    }

    public void updateDepartment(String comId, Long depNo, @Valid ReqDepartmentDto reqDepartmentDto) {

        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));
        // 1) 내 회사 부서인지 확인 + 조회
        Department department = departmentRepository.findByCompanyAndDepNo(company, depNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));



        // 2) depId 변경 시 중복 체크 (같은 부서가 자기 depId 그대로면 OK)
        String newDepId = reqDepartmentDto.getDepId();
        if (!department.getDepId().equals(newDepId)) {
            boolean exists = departmentRepository.existsByCompanyAndDepId(company, newDepId);
            if (exists) {
                throw new CustomException(ErrorCode.DUPLICATE_DEPID);
            }
        }

        // 3) 반영
        department.update(newDepId, reqDepartmentDto.getDepName());
        // JPA dirty checking으로 save() 없어도 됨
    }

    public void deleteDepartment(String comId, Long depNo) {


        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 부서 존재 체크
        Department department = departmentRepository.findById(depNo)
                .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));

        // 다른 회사 부서 삭제 방지 (중요)
        if (!department.getCompany().getComNo().equals(company.getComNo())) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 부서에 사원 남아있으면 삭제 불가
        boolean hasEmployees = employeeRepository.existsByDepartment_DepNo(depNo);
        if (hasEmployees) {
            throw new CustomException(ErrorCode.DEPARTMENT_DELETE_HAS_EMPLOYEES);
        }

        departmentRepository.delete(department);


    }
}
