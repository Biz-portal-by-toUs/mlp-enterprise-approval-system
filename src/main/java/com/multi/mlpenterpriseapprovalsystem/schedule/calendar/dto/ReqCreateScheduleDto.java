package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.dto;

import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일정 등록, 수정 요청 dto
 *
 * @author : 권지영
 * @filename : ReqCreateScheduleDto
 * @since : 2025. 12. 31. 수요일
 */
@Getter
@Setter
@NoArgsConstructor
public class ReqCreateScheduleDto {

    @NotNull(message = "scope(PERSONAL/DEPARTMENT/COMPANY)는 필수입니다.")
    private CalendarScope scope;

    @NotBlank(message = "title은 필수입니다.")
    private String title;

    private String content;

    private boolean allDay = false;

    // allDay=true일 때 사용 (날짜 기반)
    private LocalDate startDate;   // inclusive
    private LocalDate endDate;     // inclusive (선택) - 며칠짜리 올데이

    // allDay=false일 때 사용 (시간 기반)
    private LocalDateTime startAt;
    private LocalDateTime endedAt;

    // DEPARTMENT scope에서만 필요
    private Long depNo;

}
