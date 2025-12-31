package com.multi.mlpenterpriseapprovalsystem.schedule.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.multi.mlpenterpriseapprovalsystem.schedule.enums.CalendarScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 일정 등록 요청 dto
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

    @NotNull(message = "startAt은 필수입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @NotNull(message = "endedAt은 필수입니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endedAt;

    private String color;

}
