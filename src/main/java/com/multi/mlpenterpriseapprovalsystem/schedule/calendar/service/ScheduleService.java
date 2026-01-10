package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.domain.Schedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto.*;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarViewType;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.repository.ScheduleRepository;
import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.service.SearchOutboxAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 일정 관련 서비스 (통합 버전)
 *
 * @author : 권지영, 김승기
 * @filename : ScheduleService
 * @since : 2025. 12. 30. 화요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScheduleService {

    private final ScheduleRepository scheduleRepository; // EmpScheduleRepository 제거됨
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final SearchOutboxAppender searchOutboxAppender;

    @Value("${holiday.service-key}")
    private String serviceKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, List<HolidayDto>> cache = new ConcurrentHashMap<>();

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
        // ✅ Schedule 테이블에서 scope=PERSONAL 이고 등록자가 본인인 일정 조회
        List<Schedule> rows = scheduleRepository.findPersonalOverlapping(
                user.getUsername(),
                range.fromInclusive,
                range.toExclusive
        );

        return rows.stream()
                .map(s -> ResScheduleDto.fromSchedule(CalendarScope.PERSONAL, s))
                .toList();
    }

    private List<ResScheduleDto> getCompanyItems(CustomUser user, Range range) {
        // ✅ 회사 전체 일정 조회 (scope=COMPANY)
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

        Long depNo = employee.getDepartment().getDepNo();

        // ✅ 부서 일정 조회 (scope=DEPARTMENT)
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
     * 일정 삭제
     */
    @Transactional
    public void deleteItem(CustomUser user, Long schNo, ReqDeleteScheduleDto req) {
        Schedule schedule = scheduleRepository.findById(schNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));

        // 권한 체크
        if (req.getScope() == CalendarScope.PERSONAL || req.getScope() == CalendarScope.DEPARTMENT) {
            // 개인/부서 일정은 등록자 본인만 삭제 가능
            if (!schedule.getRegister().getEmpId().equals(user.getUsername())) {
                throw new CustomException(ErrorCode.NOT_REGISTER);
            }
        } else if (req.getScope() == CalendarScope.COMPANY) {
            // 회사 일정은 관리자만 삭제 가능
            if (!hasAnyRole(user, Set.of("ROLE_COM_ADMIN", "ROLE_SEC_ADMIN", "ROLE_THR_ADMIN"))) {
                throw new CustomException(ErrorCode.SCHEDULE_NOT_AUTH);
            }
        }

        scheduleRepository.delete(schedule);

        // 검색 아웃박스 동기화 (삭제)
        searchOutboxAppender.enqueueDelete(user.getComId(), SearchDocType.SCHEDULE, schNo);
    }

    /**
     * 일정 수정
     */
    @Transactional
    public void updateItem(CustomUser user, Long schNo, ReqCreateScheduleDto req) {
        Schedule schedule = scheduleRepository.findById(schNo)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));

        TimeBundle time = resolveTime(req);

        // 권한 체크
        if (req.getScope() == CalendarScope.PERSONAL || req.getScope() == CalendarScope.DEPARTMENT) {
            if (!schedule.getRegister().getEmpId().equals(user.getUsername())) {
                throw new CustomException(ErrorCode.NOT_REGISTER);
            }
        } else if (req.getScope() == CalendarScope.COMPANY) {
            if (!hasAnyRole(user, Set.of("ROLE_COM_ADMIN", "ROLE_SEC_ADMIN", "ROLE_THR_ADMIN"))) {
                throw new CustomException(ErrorCode.SCHEDULE_NOT_AUTH);
            }
        }

        // 데이터 업데이트
        schedule.update(req.getTitle(), req.getContent(), time.startAt, time.endedAt, time.allDay);

        // 알림 전송 로직
        if (req.getScope() == CalendarScope.DEPARTMENT) {
            List<Employee> emps = employeeRepository.findAllByDepartment_DepNo(schedule.getDepartment().getDepNo());
            for (Employee emp : emps) {
                notificationService.sendNotification(emp, NotificationType.CALENDER, "부서 일정", "\"" + req.getTitle() + "\" 부서 일정이 수정 되었습니다.", "/schedule/calendar");
            }
        } else if (req.getScope() == CalendarScope.COMPANY) {
            List<Employee> emps = employeeRepository.findAllByCompany_ComId(user.getComId());
            for (Employee emp : emps) {
                notificationService.sendNotification(emp, NotificationType.CALENDER, "회사 일정", "\"" + req.getTitle() + "\" 회사 일정이 수정 되었습니다.", "/schedule/calendar");
            }
        }

        // 검색 아웃박스 동기화 (수정/업서트)
        searchOutboxAppender.enqueueUpsert(user.getComId(), SearchDocType.SCHEDULE, schNo);
    }

    /**
     * 일정 생성
     */
    @Transactional
    public ResScheduleDto createItem(CustomUser user, ReqCreateScheduleDto req) {
        Employee employee = employeeRepository.findByEmpId(user.getUsername())
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String comId = user.getComId();
        TimeBundle time = resolveTime(req);

        return switch (req.getScope()) {
            case PERSONAL -> createPersonal(employee, time, req);
            case COMPANY -> createCompany(user, employee, comId, time, req);
            case DEPARTMENT -> createDepartment(employee, comId, time, req);
        };
    }

    private ResScheduleDto createPersonal(Employee employee, TimeBundle time, ReqCreateScheduleDto req) {
        String finalColor = ScheduleColorPolicy.resolve(req.getScope());

        // ✅ Schedule 엔티티로 생성 (scope = PERSONAL)
        Schedule saved = scheduleRepository.save(
                Schedule.create(CalendarScope.PERSONAL, employee.getCompany(), null, employee,
                        req.getTitle(), req.getContent(),
                        time.startAt, time.endedAt, time.allDay, finalColor)
        );

        // 개인 일정도 검색 대상이라면 추가
        searchOutboxAppender.enqueueUpsert(employee.getCompany().getComId(), SearchDocType.SCHEDULE, saved.getSchNo());

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

    private ResScheduleDto createCompany(CustomUser user, Employee register, String comId, TimeBundle time, ReqCreateScheduleDto req) {
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        if (!hasAnyRole(user, Set.of("ROLE_COM_ADMIN", "ROLE_SEC_ADMIN", "ROLE_THR_ADMIN"))) {
            throw new CustomException(ErrorCode.SCHEDULE_NOT_AUTH);
        }

        String finalColor = ScheduleColorPolicy.resolve(req.getScope());

        Schedule saved = scheduleRepository.save(
                Schedule.create(CalendarScope.COMPANY, company, null, register,
                        req.getTitle(), req.getContent(),
                        time.startAt, time.endedAt, time.allDay, finalColor)
        );

        List<Employee> emps = employeeRepository.findAllByCompany_ComId(comId);
        for (Employee emp : emps) {
            notificationService.sendNotification(emp, NotificationType.CALENDER, "회사 일정", "\"" + req.getTitle() + "\" 회사 일정이 추가 되었습니다.", "/schedule/calendar");
        }

        searchOutboxAppender.enqueueUpsert(comId, SearchDocType.SCHEDULE, saved.getSchNo());

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
        String finalColor = ScheduleColorPolicy.resolve(req.getScope());

        Schedule saved = scheduleRepository.save(
                Schedule.create(CalendarScope.DEPARTMENT, company, department, register,
                        req.getTitle(), req.getContent(),
                        time.startAt, time.endedAt, time.allDay, finalColor)
        );

        List<Employee> emps = employeeRepository.findAllByDepartment_DepNo(department.getDepNo());
        for (Employee emp : emps) {
            notificationService.sendNotification(emp, NotificationType.CALENDER, "부서 일정", "\"" + req.getTitle() + "\" 부서 일정이 추가 되었습니다.", "/schedule/calendar");
        }

        searchOutboxAppender.enqueueUpsert(comId, SearchDocType.SCHEDULE, saved.getSchNo());

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

    // --- 내부 헬퍼 메서드 및 레코드 ---

    private record Range(LocalDateTime fromInclusive, LocalDateTime toExclusive) {}
    private record TimeBundle(LocalDateTime startAt, LocalDateTime endedAt, boolean allDay) {}

    private Range resolveRange(ReqScheduleDto req) {
        if (req.getBaseDate() == null) throw new IllegalArgumentException("baseDate는 필수입니다.");
        if (req.getView() == null) throw new IllegalArgumentException("view(WEEK/MONTH)는 필수입니다.");

        LocalDate base = req.getBaseDate();
        DayOfWeek weekStart = (req.getWeekStart() == null) ? DayOfWeek.MONDAY : req.getWeekStart();

        if (req.getView() == CalendarViewType.WEEK) {
            LocalDate start = startOfWeek(base, weekStart);
            return new Range(start.atStartOfDay(), start.plusDays(7).atStartOfDay());
        }

        LocalDate firstOfMonth = base.withDayOfMonth(1);
        if (!req.isIncludeAdjacentInMonth()) {
            return new Range(firstOfMonth.atStartOfDay(), firstOfMonth.plusMonths(1).atStartOfDay());
        }

        LocalDate gridStart = startOfWeek(firstOfMonth, weekStart);
        LocalDate lastOfMonth = firstOfMonth.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate gridEndExclusive = startOfWeek(lastOfMonth, weekStart).plusDays(7);

        return new Range(gridStart.atStartOfDay(), gridEndExclusive.atStartOfDay());
    }

    private LocalDate startOfWeek(LocalDate date, DayOfWeek weekStart) {
        int current = date.getDayOfWeek().getValue();
        int start = weekStart.getValue();
        int diff = (current - start + 7) % 7;
        return date.minusDays(diff);
    }

    private TimeBundle resolveTime(ReqCreateScheduleDto req) {
        if (req.isAllDay()) {
            if (req.getStartDate() == null) throw new CustomException(ErrorCode.MUST_STARTDATE_IF_ALLDAY_TRUE);
            LocalDate end = (req.getEndDate() != null) ? req.getEndDate() : req.getStartDate();
            if (end.isBefore(req.getStartDate())) throw new CustomException(ErrorCode.START_MUST_BEFORE_END);
            return new TimeBundle(req.getStartDate().atStartOfDay(), end.plusDays(1).atStartOfDay(), true);
        }
        if (req.getStartAt() == null || req.getEndedAt() == null) throw new CustomException(ErrorCode.MUST_STARTAT_ENDEDAT);
        if (!req.getEndedAt().isAfter(req.getStartAt())) throw new CustomException(ErrorCode.START_MUST_BEFORE_END);
        return new TimeBundle(req.getStartAt(), req.getEndedAt(), false);
    }

    private boolean hasAnyRole(CustomUser user, Set<String> allowed) {
        return user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(allowed::contains);
    }
    public List<HolidayDto> getHolidays(int year, int month) {
        String key = year + "-" + String.format("%02d", month);
        return cache.computeIfAbsent(key, k -> fetch(year, month));
    }



    private List<HolidayDto> fetch(int year, int month) {
        String url = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"
                + "?serviceKey=" + UriUtils.encodeQueryParam(serviceKey, StandardCharsets.UTF_8)
                + "&solYear=" + year
                + "&solMonth=" + String.format("%02d", month)
                + "&numOfRows=100"
                + "&pageNo=1"
                + "&_type=json"; // ✅ JSON 고정

        String body = restTemplate.getForObject(url, String.class);
        if (body == null || body.isBlank()) return List.of();

        String s = body.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1).trim(); // BOM 제거

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(s);

            JsonNode header = root.path("response").path("header");
            String resultCode = header.path("resultCode").asText();
            String resultMsg  = header.path("resultMsg").asText();

            JsonNode bodyNode = root.path("response").path("body");
            int totalCount = bodyNode.path("totalCount").asInt(0);

            System.out.println("[HOLIDAY] resultCode=" + resultCode + ", resultMsg=" + resultMsg + ", totalCount=" + totalCount);

            // 정상 응답 아니면 빈값
            if (!"00".equals(resultCode)) return List.of();

            JsonNode itemNode = bodyNode.path("items").path("item");
            if (itemNode.isMissingNode() || itemNode.isNull()) return List.of();

            List<HolidayDto> out = new ArrayList<>();

            // ✅ item이 배열일 수도 / 단일 객체일 수도 있음
            if (itemNode.isArray()) {
                for (JsonNode it : itemNode) addHoliday(it, out);
            } else {
                addHoliday(itemNode, out);
            }

            System.out.println("[HOLIDAY] parsed items=" + out.size());
            return out;

        } catch (Exception e) {
            System.out.println("[HOLIDAY] JSON parse failed: " + e.getMessage());
            return List.of();
        }
    }

    private void addHoliday(JsonNode it, List<HolidayDto> out) {
        String name = it.path("dateName").asText(null);
        String isHoliday = it.path("isHoliday").asText(null);

        // locdate가 숫자(20260101)로 올 수 있음
        String locdate = it.path("locdate").isNumber()
                ? String.valueOf(it.path("locdate").asLong())
                : it.path("locdate").asText(null);

        if (locdate == null || locdate.length() != 8) return;

        // 공휴일만 쓰고 싶으면 Y만
        if (isHoliday != null && !isHoliday.isBlank() && !"Y".equalsIgnoreCase(isHoliday)) return;

        String date = locdate.substring(0, 4) + "-" + locdate.substring(4, 6) + "-" + locdate.substring(6, 8);
        out.add(new HolidayDto(date, name));
    }

}