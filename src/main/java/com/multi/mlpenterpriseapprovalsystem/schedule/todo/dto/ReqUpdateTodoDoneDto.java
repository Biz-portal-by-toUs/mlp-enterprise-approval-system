package com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투두리스트 완료 요청 dto
 *
 * @author : 권지영
 * @filename : ReqUpdateTodoDoneDto
 * @since : 2026. 1. 4. 일요일
 */
@Getter
@NoArgsConstructor
public class ReqUpdateTodoDoneDto {
    @NotNull
    private Boolean isDone;
}
