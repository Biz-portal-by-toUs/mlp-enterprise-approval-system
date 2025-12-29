package com.multi.mlpenterpriseapprovalsystem.board.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardResAllDto;
import com.multi.mlpenterpriseapprovalsystem.board.dto.CommentDto;
import com.multi.mlpenterpriseapprovalsystem.board.service.CommentService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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

    @PostMapping(value ="/comment", consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseDto> regiComment(@ModelAttribute CommentDto dto, @AuthenticationPrincipal CustomUser customUser) {

        dto.setComId(customUser.getComId());
        dto.setEmpId(customUser.getUsername());
        dto.setCreatedAt(LocalDateTime.now());
        commentService.registComment(dto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<BoardResAllDto>(HttpStatus.OK, "댓글 등록 성공", null));

    }


}
