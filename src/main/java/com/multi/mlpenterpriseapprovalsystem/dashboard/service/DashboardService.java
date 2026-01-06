package com.multi.mlpenterpriseapprovalsystem.dashboard.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.dashboard.dto.ResDashboardDto;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.repository.ApprovalLineRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.mail.repository.MailUserStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메인페이지 대시보드 메일, 결재 관련 요약 서비스
 *
 * @author : 권지영
 * @filename : DashboardService
 * @since : 2026. 1. 6. 화요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {
    private final MailUserStateRepository mailUserStateRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final EmployeeRepository employeeRepository;

    public ResDashboardDto getSummary(CustomUser user) {

        Employee emp = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        // 1. 안 읽은 메일 개수 조회
        int unreadMailCount = mailUserStateRepository.countByUserAndIsReadFalse(emp);

        // 2. 결재 대기 중인 문서 개수 조회 (ApprStat.AWAITING 가정)
        int awaitingApprovalCount = approvalLineRepository.countByApproverAndApprStat(emp, ApprStat.I);

        return ResDashboardDto.builder()
                .newMail(unreadMailCount)
                .newApproval(awaitingApprovalCount)
                .build();
    }
}
