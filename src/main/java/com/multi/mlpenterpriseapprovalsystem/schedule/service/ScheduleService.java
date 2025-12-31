package com.multi.mlpenterpriseapprovalsystem.schedule.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.schedule.domain.EmpSchedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.domain.Schedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ReqScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ResScheduleDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.dto.ResScheduleListDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.enums.CalendarScope;
import com.multi.mlpenterpriseapprovalsystem.schedule.repository.EmpScheduleRepository;
import com.multi.mlpenterpriseapprovalsystem.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
public class ScheduleService {

    private final EmpScheduleRepository empScheduleRepository;
    private final ScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;

    public ResScheduleListDto getItems(CustomUser user, ReqScheduleDto req) {
        Range range = switch (req.getView()) {
            case WEEK -> calcWeekRange(req.getBaseDate(), req.getWeekStart());
            case MONTH -> calcMonthRange(req.getBaseDate(), req.getWeekStart(), req.isIncludeAdjacentInMonth());
        };

        if(user == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String empId = user.getUsername();
        String comId = user.getComId();

        Employee employee = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Long depNo = employee.getDepartment().getDepNo();

        List<ResScheduleDto> items = switch (req.getScope()) {
            case PERSONAL -> {
                List<EmpSchedule> list = empScheduleRepository.findOverlapping(
                        empId, range.fromInclusive(), range.toExclusive()
                );
                yield list.stream().map(ResScheduleDto::fromPersonal).toList();
            }
            case COMPANY -> {
                List<Schedule> list = scheduleRepository.findCompanyOverlapping(
                        comId, range.fromInclusive(), range.toExclusive()
                );
                yield list.stream().map(s -> ResScheduleDto.fromSchedule(CalendarScope.COMPANY, s)).toList();
            }
            case DEPARTMENT -> {
                List<Schedule> list = scheduleRepository.findDepartmentOverlapping(
                        depNo, range.fromInclusive(), range.toExclusive()
                );
                yield list.stream().map(s -> ResScheduleDto.fromSchedule(CalendarScope.DEPARTMENT, s)).toList();
            }
        };

        return new ResScheduleListDto(range.fromInclusive(), range.toExclusive(), items);
    }

    private Range calcWeekRange(LocalDate baseDate, DayOfWeek weekStart) {
        LocalDate start = baseDate.with(TemporalAdjusters.previousOrSame(weekStart));
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = start.plusDays(7).atStartOfDay(); // exclusive
        return new Range(from, to);
    }

    private Range calcMonthRange(LocalDate baseDate, DayOfWeek weekStart, boolean includeAdjacent) {
        YearMonth ym = YearMonth.from(baseDate);
        LocalDate monthStart = ym.atDay(1);
        LocalDate nextMonthStart = ym.plusMonths(1).atDay(1);

        if (!includeAdjacent) {
            return new Range(monthStart.atStartOfDay(), nextMonthStart.atStartOfDay());
        }

        LocalDate fromDate = monthStart.with(TemporalAdjusters.previousOrSame(weekStart));

        LocalDate lastDay = nextMonthStart.minusDays(1);
        LocalDate toDate = lastDay.with(TemporalAdjusters.next(weekStart));
        return new Range(fromDate.atStartOfDay(), toDate.atStartOfDay());
    }

    private record Range(LocalDateTime fromInclusive, LocalDateTime toExclusive) {}
}