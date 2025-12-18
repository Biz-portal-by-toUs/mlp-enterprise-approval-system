package com.multi.mlpenterpriseapprovalsystem.auth.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.service.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * employeeUserDetailSerivce
 *
 * @author : 권지영
 * @filename : EmployeeUserDetailService
 * @since : 2025. 12. 17. 수요일
 */
@Service
@RequiredArgsConstructor
public class EmployeeUserDetailService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String empId) throws UsernameNotFoundException {
        Employee emp = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new UsernameNotFoundException("사원 없음: " + empId));

        return CustomUser.builder()
                .subjectId(emp.getEmpNo()) // 이메일로 쓰면 나중에 뭐 변환해야해서 pk로 사용
                .subjectType(TokenSubjectType.EMPLOYEE)
                .comId(emp.getCompany().getComId())
                .username(emp.getEmpId())
                .password(emp.getPwd())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + emp.getRole())))
                .build();
    }
}
