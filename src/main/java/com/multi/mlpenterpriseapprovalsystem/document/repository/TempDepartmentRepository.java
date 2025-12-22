package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempDepartmentRepository
 * @since : 25. 12. 20. 토요일
 */
public interface TempDepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByDepNameAndCompany_comId(String comId, String depName);
}
