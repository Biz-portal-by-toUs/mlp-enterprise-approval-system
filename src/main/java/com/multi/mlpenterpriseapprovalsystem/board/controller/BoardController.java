package com.multi.mlpenterpriseapprovalsystem.board.controller;

import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardCatDto;
import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardReqDto;
import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardResAllDto;
import com.multi.mlpenterpriseapprovalsystem.board.dto.CommentDto;
import com.multi.mlpenterpriseapprovalsystem.board.service.BoardService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardController
 * @since : 2025-12-18 목요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class BoardController {

    private final BoardService boardService;

    //게시글 카테고리 검색
    @GetMapping("/boards/category")
    public ResponseEntity<ResponseDto<List<BoardCatDto>>> getAll() {

        return ResponseEntity.ok(new ResponseDto<List<BoardCatDto>>(HttpStatus.OK, "카탈로그 조회 성공", boardService.getAllCategory()));
    }


    //회사별 페이징처리
    @GetMapping("/boards-all")
    public ResponseEntity<ResponseDto<Page<BoardResAllDto>>> getBoardForAllByCompany(@RequestParam(name = "page", defaultValue = "0") int page,
                                                                                       @RequestParam(name = "size", defaultValue = "10") int size
                                                                                        ) {
        String comId = "A01";
        Pageable pageable = PageRequest.of(page, size, Sort.by("boardNo").descending());
        Page<BoardResAllDto> boards = boardService.selectBoardListWithPagingForAllByCompany(pageable,comId);

        return ResponseEntity.ok().body(new ResponseDto<Page<BoardResAllDto>>(HttpStatus.OK, "조회 성공", boards));
    }

    //특정 게시판(Board)에 대한 댓글(Commnet) 목록을 조회
    @GetMapping("/boards/{boardNo}/comments")
    public ResponseEntity<ResponseDto<List<CommentDto>>> getBoardById(@PathVariable Long boardNo) {

        return ResponseEntity.ok().body(new ResponseDto<List<CommentDto>>(HttpStatus.OK, "댓글 조회 성공", boardService.getCommentByBoardNo(boardNo)));
    }

    //게시글 삭제
    @DeleteMapping("/boards/{boardNo}")
    public ResponseEntity<String> delete(@PathVariable("boardNo") Long boardNo) {
        boardService.deleteBoard(boardNo);
        return ResponseEntity.ok("게시판이 삭제되었습니다.");
    }

    //게시글 변경
    @PutMapping("/boards/{boardNo}")
    public ResponseEntity<String> update(@PathVariable("boardNo") Long boardNo, @RequestBody @Valid BoardReqDto dto) {

        boardService.updateBoard(boardNo, dto);
        return ResponseEntity.ok("상품이 수정되었습니다.");
    }

    // 게시글 작성
    @PostMapping("/boards")
    public ResponseEntity<String> getBoardById(@RequestBody @Valid BoardReqDto dto) {

        boardService.registBoard(dto);
        return ResponseEntity.created(URI.create("/api/v1/boards-all/{comid}")).build();
    }

    //자유 게시글 상세조회  --- 일련번호(key로 조회)
    @GetMapping("/boards/{boardNo}")
    public ResponseEntity<ResponseDto<BoardResAllDto>> detail(@PathVariable("boardNo") Long boardNo) {
        return ResponseEntity.ok().body(new ResponseDto<BoardResAllDto>(HttpStatus.OK, "조회 성공", boardService.detailBoard(boardNo)));
    }


}

