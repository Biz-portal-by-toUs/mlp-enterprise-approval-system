package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto;

import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope;
import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarViewType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 일정 조회 요청 dto
 *
 * @author : 권지영
 * @filename : ReqScheduleDto
 * @since : 2025. 12. 30. 화요일
 */
@Getter
@Setter
@NoArgsConstructor
public class ReqScheduleDto {

    @NotNull(message = "scope(PERSONAL/DEPARTMENT/COMPANY)는 필수입니다.")
    private CalendarScope scope;

    @NotNull(message = "view(WEEK/MONTH)는 필수입니다.")
    private CalendarViewType view;

    @NotNull(message = "baseDate는 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate baseDate;

    private DayOfWeek weekStart = DayOfWeek.MONDAY;
    private boolean includeAdjacentInMonth = true;
}
