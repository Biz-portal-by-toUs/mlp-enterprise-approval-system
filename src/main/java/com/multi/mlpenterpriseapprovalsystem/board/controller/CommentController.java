package com.multi.mlpenterpriseapprovalsystem.board.controller;

import com.multi.mlpenterpriseapprovalsystem.board.dto.CommentDto;
import com.multi.mlpenterpriseapprovalsystem.board.service.CommentService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : CommentController
 * @since : 2025-12-19 금요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/comments/{commentNo}")
    public ResponseEntity<ResponseDto<CommentDto>> getComment(@PathVariable(name="commentNo") Long commentNo) {
        return ResponseEntity.ok().body(new ResponseDto<CommentDto>(HttpStatus.OK, "댓글 조회 성공", commentService.getCommentById(commentNo)));
    }

    @DeleteMapping("/comments/{commentNo}")
    public ResponseEntity<String> delete(@PathVariable(name="commentNo") Long commentNo) {
        commentService.deleteComment(commentNo);
        return ResponseEntity.ok("댓글이 삭제되었습니다.");
    }


}
