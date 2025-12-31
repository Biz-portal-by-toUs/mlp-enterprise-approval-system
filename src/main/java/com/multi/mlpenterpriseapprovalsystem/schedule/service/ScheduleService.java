package com.multi.mlpenterpriseapprovalsystem.schedule.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.schedule.domain.EmpSchedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.domain.Schedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ReqCreateScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ReqScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ResScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ResScheduleListDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.enums.CalendarScope;
import com.multi.mlpenterpriseapprovalsystem.schedule.enums.CalendarViewType;
import com.multi.mlpenterpriseapprovalsystem.schedule.repository.EmpScheduleRepository;
import com.multi.mlpenterpriseapprovalsystem.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 일정 관련 서비스
 *
 * @author : 권지영
 * @filename : EmpScheduleService
 * @since : 2025. 12. 30. 화요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScheduleService {

    private final EmpScheduleRepository empScheduleRepository;
    private final ScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    public ResScheduleListDto getItems(CustomUser user, ReqScheduleDto req) {
        Range range = resolveRange(req);

        List<ResScheduleDto> items = switch (req.getScope()) {
            case PERSONAL -> getPersonalItems(user, range);
            case COMPANY -> getCompanyItems(user, range);
            case DEPARTMENT -> getDepartmentItems(user, range);
        };

        return ResScheduleListDto.builder()
                .fromInclusive(range.fromInclusive)
                .toExclusive(range.toExclusive)
                .items(items)
                .build();
    }

    private List<ResScheduleDto> getPersonalItems(CustomUser user, Range range) {
        // ✅ 로그인 사용자 empId는 토큰의 username(예: E000001) 기준으로 조회
        // repository는 emp_id(사번) 기준으로 겹치는 일정 조회한다고 가정
        List<EmpSchedule> rows = empScheduleRepository.findOverlapping(
                user.getUsername(),
                range.fromInclusive,
                range.toExclusive
        );

        return rows.stream()
                .map(ResScheduleDto::fromPersonal)
                .toList();
    }

    private List<ResScheduleDto> getCompanyItems(CustomUser user, Range range) {
        // ✅ 회사 일정: department is null + 회사 com_id 필터
        List<Schedule> rows = scheduleRepository.findCompanyOverlapping(
                user.getComId(),
                range.fromInclusive,
                range.toExclusive
        );

        return rows.stream()
                .map(s -> ResScheduleDto.fromSchedule(CalendarScope.COMPANY, s))
                .toList();
    }

    private List<ResScheduleDto> getDepartmentItems(CustomUser user, Range range) {
        Employee employee = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        log.info("조회>>>>>>>>>>>>>" + employee.getEmpId());

        Long depNo = employee.getDepartment().getDepNo();

        List<Schedule> rows = scheduleRepository.findDepartmentOverlapping(
                user.getComId(),
                depNo,
                range.fromInclusive,
                range.toExclusive
        );

        return rows.stream()
                .map(s -> ResScheduleDto.fromSchedule(CalendarScope.DEPARTMENT, s))
                .toList();
    }

    /**
     * WEEK/MONTH 조회 범위 계산
     * - WEEK: weekStart 기준 주 시작 00:00 ~ +7일 00:00
     * - MONTH:
     *   - includeAdjacentInMonth=false: 해당 월 1일 00:00 ~ 다음달 1일 00:00
     *   - includeAdjacentInMonth=true: 달력 그리드 전체(앞/뒤 인접일 포함)
     */
    private Range resolveRange(ReqScheduleDto req) {
        if (req.getBaseDate() == null) {
            throw new IllegalArgumentException("baseDate는 필수입니다.");
        }
        if (req.getView() == null) {
            throw new IllegalArgumentException("view(WEEK/MONTH)는 필수입니다.");
        }

        LocalDate base = req.getBaseDate();
        DayOfWeek weekStart = (req.getWeekStart() == null) ? DayOfWeek.MONDAY : req.getWeekStart();

        if (req.getView() == CalendarViewType.WEEK) {
            LocalDate start = startOfWeek(base, weekStart);
            return new Range(start.atStartOfDay(), start.plusDays(7).atStartOfDay());
        }

        // MONTH
        LocalDate firstOfMonth = base.withDayOfMonth(1);
        LocalDate firstOfNextMonth = firstOfMonth.plusMonths(1);

        if (!req.isIncludeAdjacentInMonth()) {
            return new Range(firstOfMonth.atStartOfDay(), firstOfNextMonth.atStartOfDay());
        }

        // 인접일 포함(달력 grid)
        LocalDate gridStart = startOfWeek(firstOfMonth, weekStart);
        LocalDate lastOfMonth = firstOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate gridEndExclusive = startOfWeek(lastOfMonth, weekStart).plusDays(7);

        return new Range(gridStart.atStartOfDay(), gridEndExclusive.atStartOfDay());
    }

    private LocalDate startOfWeek(LocalDate date, DayOfWeek weekStart) {
        int current = date.getDayOfWeek().getValue(); // MON=1..SUN=7
        int start = weekStart.getValue();
        int diff = (current - start + 7) % 7;
        return date.minusDays(diff);
    }

    private record Range(LocalDateTime fromInclusive, LocalDateTime toExclusive) {}

    @Transactional
    public ResScheduleDto createItem(CustomUser user, ReqCreateScheduleDto req) {

        String empId = user.getUsername();
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        log.info("등록>>>>>>>>>>>>>" + employee.getEmpId());
        String comId = user.getComId();

        TimeBundle time = resolveTime(req);

        return switch (req.getScope()) {
            case PERSONAL -> createPersonal(employee, time, req);
            case COMPANY -> createCompany(employee, comId, time, req);
            case DEPARTMENT -> createDepartment(employee, comId, time, req);
        };
    }

    private record TimeBundle(LocalDateTime startAt, LocalDateTime endedAt, boolean allDay) {}

    private TimeBundle resolveTime(ReqCreateScheduleDto req) {
        if (req.isAllDay()) {
            if (req.getStartDate() == null) {
                throw new CustomException(ErrorCode.MUST_STARTDATE_IF_ALLDAY_TRUE);
            }
            LocalDate end = (req.getEndDate() != null) ? req.getEndDate() : req.getStartDate();
            if (end.isBefore(req.getStartDate())) {
                throw new CustomException(ErrorCode.START_MUST_BEFORE_END);
            }

            LocalDateTime startAt = req.getStartDate().atStartOfDay();
            LocalDateTime endedAt = end.plusDays(1).atStartOfDay(); // ✅ end는 exclusive
            return new TimeBundle(startAt, endedAt, true);
        }

        if (req.getStartAt() == null || req.getEndedAt() == null) {
            throw new CustomException(ErrorCode.MUST_STARTAT_ENDEDAT);
        }
        if (!req.getEndedAt().isAfter(req.getStartAt())) {
            throw new CustomException(ErrorCode.START_MUST_BEFORE_END);
        }
        return new TimeBundle(req.getStartAt(), req.getEndedAt(), false);
    }

    private ResScheduleDto createPersonal(Employee employee, TimeBundle time, ReqCreateScheduleDto req) {
        EmpSchedule saved = empScheduleRepository.save(
                EmpSchedule.create(employee, req.getTitle(), req.getContent(),
                        time.startAt, time.endedAt, time.allDay, req.getColor())
        );

        return ResScheduleDto.builder()
                .schNo(saved.getSchNo())
                .scope(CalendarScope.PERSONAL)
                .title(saved.getTitle())
                .content(saved.getContent())
                .startAt(saved.getStartAt())
                .endedAt(saved.getEndedAt())
                .color(saved.getColor())
                .build();
    }

    private ResScheduleDto createCompany(Employee register, String comId, TimeBundle time, ReqCreateScheduleDto req) {
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Schedule saved = scheduleRepository.save(
                Schedule.create(company, null, register,
                        req.getTitle(), req.getContent(),
                        time.startAt, time.endedAt, time.allDay, req.getColor())
        );

        return ResScheduleDto.builder()
                .schNo(saved.getSchNo())
                .scope(CalendarScope.COMPANY)
                .title(saved.getTitle())
                .content(saved.getContent())
                .startAt(saved.getStartAt())
                .endedAt(saved.getEndedAt())
                .color(saved.getColor())
                .build();
    }

    private ResScheduleDto createDepartment(Employee register, String comId, TimeBundle time, ReqCreateScheduleDto req) {
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        Department department = register.getDepartment();

        Schedule saved = scheduleRepository.save(
                Schedule.create(company, department, register,
                        req.getTitle(), req.getContent(),
                        time.startAt, time.endedAt, time.allDay, req.getColor())
        );

        return ResScheduleDto.builder()
                .schNo(saved.getSchNo())
                .scope(CalendarScope.DEPARTMENT)
                .title(saved.getTitle())
                .content(saved.getContent())
                .startAt(saved.getStartAt())
                .endedAt(saved.getEndedAt())
                .color(saved.getColor())
                .build();
    }
}