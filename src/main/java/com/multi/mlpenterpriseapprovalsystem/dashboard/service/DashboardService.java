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
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto.ReqScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto.ResScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto.ResScheduleListDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarViewType;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private final ScheduleService scheduleService;

    public ResDashboardDto getSummary(CustomUser user) {

        Employee emp = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        // 1. 안 읽은 메일 개수 조회
        int unreadMailCount = (int) mailUserStateRepository.countUnreadInboxOnly(emp.getEmpId());

        // 2. 결재 대기 중인 문서 개수 조회 (ApprStat.AWAITING 가정)
        int awaitingApprovalCount = approvalLineRepository.countByApproverAndApprStat(emp, ApprStat.I);

        return ResDashboardDto.builder()
                .newMail(unreadMailCount)
                .newApproval(awaitingApprovalCount)
                .build();
    }

    /**
     * 대시보드용 통합 일정 조회
     * 회사(COMPANY), 부서(DEPARTMENT), 개인(PERSONAL) 일정을 모두 가져와 합칩니다.
     */
    public List<ResScheduleDto> getCombinedSchedules(CustomUser user, LocalDate baseDate) {
        List<ResScheduleDto> combinedItems = new ArrayList<>();
        CalendarScope[] scopes = {CalendarScope.COMPANY, CalendarScope.DEPARTMENT, CalendarScope.PERSONAL};

        for (CalendarScope scope : scopes) {
            ReqScheduleDto request = new ReqScheduleDto();
            request.setScope(scope);
            request.setView(CalendarViewType.WEEK); // 혹은 MONTH
            request.setBaseDate(baseDate);

            try {
                // 기존 서비스 호출
                ResScheduleListDto result = scheduleService.getItems(user, request);

                // ✅ 결과 객체와 내부 리스트가 null이 아닌지 엄격히 확인
                if (result != null && result.getItems() != null) {
                    combinedItems.addAll(result.getItems());
                }
            } catch (Exception e) {
                // 특정 스코프 조회 실패 시 로그를 남기고 다음 스코프로 진행 (500 에러 방지)
                log.error("대시보드 일정 조회 실패 [영역: {}]: {}", scope, e.getMessage());
            }
        }
        return combinedItems;
    }
}
