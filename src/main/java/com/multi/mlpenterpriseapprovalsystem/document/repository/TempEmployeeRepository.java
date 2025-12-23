package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempEmployeeRepository
 * @since : 25. 12. 22. 월요일
 */
public interface TempEmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmpId(String myEmpId);
}
