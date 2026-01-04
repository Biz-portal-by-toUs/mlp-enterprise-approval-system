package com.multi.mlpenterpriseapprovalsystem.schedule.todo.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.domain.TodoList;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto.ReqCreateTodoDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto.ResTodoDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 투두리스트 서비스
 *
 * @author : 권지영
 * @filename : TodoService
 * @since : 2026. 1. 4. 일요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final EmployeeRepository employeeRepository;
    private final TodoRepository todoRepository;

    @Transactional
    public ResTodoDto create(String empId, ReqCreateTodoDto req) {
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        TodoList todo = TodoList.create(employee, req.getTitle());


        TodoList saved = todoRepository.save(todo);
        return ResTodoDto.from(saved);
    }
}
