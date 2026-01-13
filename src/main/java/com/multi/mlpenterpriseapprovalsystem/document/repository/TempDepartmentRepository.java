package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 임시 부서 테이블 관리 레포지토리
 *
 * @author : 이지헌
 * @filename : TempDepartmentRepository
 * @since : 25. 12. 20. 토요일
 */
public interface TempDepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByDepNameAndCompany_comId(String comId, String depName);
    List<Department> findAllByCompany_comId(String comId);

}
