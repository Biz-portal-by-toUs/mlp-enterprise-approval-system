package com.multi.mlpenterpriseapprovalsystem.employee.service;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Employee repository
 *
 * @author : 권지영
 * @filename : EmployeeRepository
 * @since : 2025. 12. 17. 수요일
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {


    Optional<Employee> findByEmpId(String empId);
}
