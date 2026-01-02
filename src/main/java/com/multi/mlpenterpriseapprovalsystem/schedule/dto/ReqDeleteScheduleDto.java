package com.multi.mlpenterpriseapprovalsystem.schedule.dto;

import com.multi.mlpenterpriseapprovalsystem.schedule.enums.CalendarScope;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일정 삭제 요청 dto
 *
 * @author : 권지영
 * @filename : ReqDeleteScheduleDto
 * @since : 2025. 12. 31. 수요일
 */
@Getter
@Setter
@NoArgsConstructor
public class ReqDeleteScheduleDto {
    @NotNull(message = "scope(PERSONAL/DEPARTMENT/COMPANY)는 필수입니다.")
    private CalendarScope scope;
}
