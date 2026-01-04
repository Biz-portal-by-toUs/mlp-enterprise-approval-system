package com.multi.mlpenterpriseapprovalsystem.schedule.todo.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.dto.*;
import com.multi.mlpenterpriseapprovalsystem.schedule.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 투두리스트 컨트롤러
 *
 * @author : 권지영
 * @filename : TodoController
 * @since : 2026. 1. 4. 일요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/todos")
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

    @PatchMapping("/{todoNo}")
    public ResponseEntity<ResponseDto<ResTodoDto>> update(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="todoNo") Long todoNo,
            @RequestBody @Valid ReqUpdateTodoDto req
    ) {
        ResTodoDto updated = todoService.update(user.getUsername(), todoNo, req);

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "투두 수정 완료", updated)
        );
    }

    @PatchMapping("/{todoNo}/done")
    public ResponseEntity<ResponseDto<ResTodoDto>> updateDone(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="todoNo") Long todoNo,
            @RequestBody @Valid ReqUpdateTodoDoneDto req
    ) {
        ResTodoDto updated = todoService.updateDone(user.getUsername(), todoNo, req.getIsDone());

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "투두 완료 상태 변경 완료", updated)
        );
    }

    @DeleteMapping("/{todoNo}")
    public ResponseEntity<ResponseDto<Void>> delete(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="todoNo") Long todoNo
    ) {
        todoService.delete(user.getUsername(), todoNo);

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "투두 삭제 완료", null)
        );
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<ResTodoDto>>> list(
            @AuthenticationPrincipal CustomUser user
    ) {
        List<ResTodoDto> grouped = todoService.listMyTodos(user.getUsername());

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "투두 목록 조회 완료", grouped)
        );
    }

}
