package com.multi.mlpenterpriseapprovalsystem.auth.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.ReqChangeMyPasswordDto;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.ResChangePasswordDto;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyIdentityVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqEmployeeIdentityVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Please explain the class!!!
 *
 * @author : 권지영
 * @filename : PasswordService
 * @since : 2025. 12. 29. 월요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    public void verifyCompanyIdentity(CustomUser user, ReqCompanyIdentityVerifyDto req) {

        if (user.getSubjectType() != TokenSubjectType.COMPANY) {
            throw new CustomException(ErrorCode.ONLY_COMPANY);
        }

        Long comNo = user.getSubjectId(); // ✅ 너 CustomUser에 맞게 변경 (ex: getComNo(), getSubjectNo() 등)
        Company company = companyRepository.findById(comNo)
                .orElseThrow(() -> new IllegalArgumentException("회사 정보가 없습니다."));

        if (!req.getEmail().equals(company.getEmail())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    public void verifyEmployeeIdentity(CustomUser user, ReqEmployeeIdentityVerifyDto req) {

        if (user.getSubjectType() != TokenSubjectType.EMPLOYEE) {
            throw new CustomException(ErrorCode.ONLY_EMPLOYEE);
        }

        Long empNo = user.getSubjectId(); // ✅ 너 CustomUser에 맞게 변경
        Employee emp = employeeRepository.findById(empNo)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        boolean ok =
                req.getEmpId().equals(emp.getEmpId()) &&
                        req.getEmpName().equals(emp.getEmpName()) &&
                        req.getEmail().equals(emp.getEmail());

        if (!ok) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    public ResChangePasswordDto changePassword(CustomUser user, @Valid ReqChangeMyPasswordDto req) {

        // 1) 새 비번/확인 일치 체크
        if (req == null || !req.isNewPasswordConfirmed()) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 2) subjectType 분기
        TokenSubjectType type = user.getSubjectType(); // ✅ 너가 하던 부분: getSubjectType()로 호출해야 함(괄호!)
        if (type == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // ✅ 보통 CustomUser가 "누구인지" PK를 들고 있음
        // 예: 회사면 comNo, 사원이면 empNo
        Long subjectNo = user.getSubjectId(); // <- 너 프로젝트에 맞게 이름 바꿔줘 (ex: getComNo(), getEmpNo())
        if (subjectNo == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        switch (type) {
            case COMPANY -> changeCompanyPassword(subjectNo, req);
            case EMPLOYEE -> changeEmployeePassword(subjectNo, req);
            default -> throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ResChangePasswordDto.ok();
    }

    private void changeCompanyPassword(Long comNo, ReqChangeMyPasswordDto req) {
        Company c = companyRepository.findById(comNo)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 현재 비번 검증
        if (!passwordEncoder.matches(req.getCurrentPassword(), c.getPwd())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 같은 비번 방지(원하면 유지)
        if (passwordEncoder.matches(req.getNewPassword(), c.getPwd())) {
            throw new CustomException(ErrorCode.ALREADY_USE_PASSWORD);
        }

        c.changePassword(passwordEncoder.encode(req.getNewPassword()));
    }

    private void changeEmployeePassword(Long empNo, ReqChangeMyPasswordDto req) {
        Employee e = employeeRepository.findByEmpNoAndIsDeletedFalse(empNo)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // 현재 비번 검증
        if (!passwordEncoder.matches(req.getCurrentPassword(), e.getPwd())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 같은 비번 방지(원하면 유지)
        if (passwordEncoder.matches(req.getNewPassword(), e.getPwd())) {
            throw new CustomException(ErrorCode.ALREADY_USE_PASSWORD);
        }

        e.changePassword(passwordEncoder.encode(req.getNewPassword()));
    }
}
