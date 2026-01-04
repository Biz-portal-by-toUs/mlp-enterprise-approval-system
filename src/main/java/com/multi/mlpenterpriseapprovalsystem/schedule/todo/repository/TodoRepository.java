package com.multi.mlpenterpriseapprovalsystem.schedule.todo.repository;

import com.multi.mlpenterpriseapprovalsystem.schedule.todo.domain.TodoList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 투두리스트 레포지토리
 *
 * @author : 권지영
 * @filename : TodoRepository
 * @since : 2026. 1. 4. 일요일
 */
public interface TodoRepository extends JpaRepository<TodoList, Long> {

    Optional<TodoList> findByTodoNoAndEmployeeEmpId(Long todoNo, String empId);
}
