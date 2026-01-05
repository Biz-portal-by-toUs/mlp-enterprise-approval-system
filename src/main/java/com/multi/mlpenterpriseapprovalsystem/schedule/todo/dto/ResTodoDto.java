package com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto;

import com.multi.mlpenterpriseapprovalsystem.schedule.todo.domain.TodoList;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 투두리스트 반환 dto
 *
 * @author : 권지영
 * @filename : ResTodoDto
 * @since : 2026. 1. 4. 일요일
 */
@Getter
public class ResTodoDto {
    private final Long todoNo;
    private final String empId;
    private final String title;
    private final Boolean isDone;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public ResTodoDto(Long todoNo, String empId, String title, Boolean isDone,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.todoNo = todoNo;
        this.empId = empId;
        this.title = title;
        this.isDone = isDone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ResTodoDto from(TodoList t) {
        return new ResTodoDto(
                t.getTodoNo(),
                t.getEmployee().getEmpId(),
                t.getTitle(),
                t.getIsDone(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
