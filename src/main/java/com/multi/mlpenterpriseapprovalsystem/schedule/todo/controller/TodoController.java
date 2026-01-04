package com.multi.mlpenterpriseapprovalsystem.schedule.todo.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto.ReqCreateTodoDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto.ResTodoDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * 투두리스트 컨트롤러
 *
 * @author : 권지영
 * @filename : TodoController
 * @since : 2026. 1. 4. 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/todo")
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<ResponseDto<ResTodoDto>> create(
            @AuthenticationPrincipal CustomUser user,
            @RequestBody @Valid ReqCreateTodoDto req
    ) {

        ResTodoDto created = todoService.create(user.getUsername(), req);

        URI location = URI.create("/api/v1/todos/" + created.getTodoNo());

        return ResponseEntity
                .created(location) // ✅ 201 + Location
                .body(new ResponseDto<>(HttpStatus.CREATED, "투두 생성 완료", created));
    }
}
