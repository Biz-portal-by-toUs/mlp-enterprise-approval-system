package com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 투두리스트 등록 요청 dto
 *
 * @author : 권지영
 * @filename : ReqCreateTodoDto
 * @since : 2026. 1. 4. 일요일
 */
@Getter
@NoArgsConstructor
public class ReqCreateTodoDto {

    @NotBlank
    @Size(max = 255)
    private String title;
}
