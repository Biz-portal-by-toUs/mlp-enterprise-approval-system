package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto;

import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.domain.EmpSchedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.domain.Schedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 사원 개인일정 반환 dto
 *
 * @author : 권지영
 * @filename : ResEmpScheduleDto
 * @since : 2025. 12. 30. 화요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResScheduleDto {

    private CalendarScope scope;
    private Long schNo;

    private String title;
    private String content;

    private LocalDateTime startAt;
    private LocalDateTime endedAt;

    private boolean allDay;

    // ✅ allDay=true일 때만 채워서 수정 폼 편하게
    private LocalDate startDate; // inclusive
    private LocalDate endDate;   // inclusive

    private String color;

    // scope별 옵션 필드
    private String empId; // PERSONAL일 때 세팅
    private Long depNo;   // DEPARTMENT일 때 세팅 (COMPANY면 null)

    private static LocalDate toStartDateIfAllDay(boolean allDay, LocalDateTime startAt) {
        return allDay ? startAt.toLocalDate() : null;
    }

    private static LocalDate toEndDateIfAllDay(boolean allDay, LocalDateTime endedAt) {
        // endedAt은 exclusive로 저장되어 있으니, inclusive endDate로 내려주려면 -1일
        return allDay ? endedAt.toLocalDate().minusDays(1) : null;
    }

    public static ResScheduleDto fromPersonal(EmpSchedule s) {
        boolean allDay = s.isAllDay();

        return ResScheduleDto.builder()
                .scope(CalendarScope.PERSONAL)
                .schNo(s.getSchNo())
                .title(s.getTitle())
                .content(s.getContent())
                .startAt(s.getStartAt())
                .endedAt(s.getEndedAt())
                .allDay(allDay)
                .startDate(toStartDateIfAllDay(allDay, s.getStartAt()))
                .endDate(toEndDateIfAllDay(allDay, s.getEndedAt()))
                .color(s.getColor())
                .empId(s.getEmployee().getEmpId())
                .depNo(null)
                .build();
    }

    public static ResScheduleDto fromSchedule(CalendarScope scope, Schedule s) {
        boolean allDay = s.isAllDay();

        return ResScheduleDto.builder()
                .scope(scope) // COMPANY or DEPARTMENT
                .schNo(s.getSchNo())
                .title(s.getTitle())
                .content(s.getContent())
                .startAt(s.getStartAt())
                .endedAt(s.getEndedAt())
                .allDay(allDay)
                .startDate(toStartDateIfAllDay(allDay, s.getStartAt()))
                .endDate(toEndDateIfAllDay(allDay, s.getEndedAt()))
                .color(s.getColor())
                .empId(null)
                // ✅ COMPANY면 department가 null일 수 있으니까 안전하게
                .depNo(s.getDepartment() == null ? null : s.getDepartment().getDepNo())
                .build();
    }
}