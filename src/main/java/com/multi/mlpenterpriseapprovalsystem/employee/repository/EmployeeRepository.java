package com.multi.mlpenterpriseapprovalsystem.employee.repository;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Employee db 접근하는 레포지토리
 *
 * @author : 김승기
 * @filename : EmployeeRepository
 * @since : 2025. 12. 17. 수요일
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmpId(String empId);
}
