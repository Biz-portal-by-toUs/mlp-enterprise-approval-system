package com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투두리스트 수정 요청 dto
 *
 * @author : 권지영
 * @filename : ReqUpdateTodoDto
 * @since : 2026. 1. 4. 일요일
 */
@Getter
@NoArgsConstructor
public class ReqUpdateTodoDto {

    @Size(max = 255)
    private String title;

    private Boolean isDone; // 부분 수정
}
