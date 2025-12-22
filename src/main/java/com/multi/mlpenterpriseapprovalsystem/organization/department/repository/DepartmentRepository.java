package com.multi.mlpenterpriseapprovalsystem.organization.department.repository;

import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 부서 db 접근 레포지토리
 *
 * @author : 권지영
 * @filename : DepartmentRepository
 * @since : 2025. 12. 22. 월요일
 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByCompanyAndDepId(Company company, String depId);

    boolean existsByCompanyAndDepName(Company company, String depName);

    List<Department> findAllByCompany_ComId(String comId);
}
