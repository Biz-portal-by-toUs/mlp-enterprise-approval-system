package com.multi.mlpenterpriseapprovalsystem.board.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.board.dto.*;
import com.multi.mlpenterpriseapprovalsystem.board.service.BoardService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    public ResponseEntity<ResponseDto<Page<BoardResAllDto>>> getBoardForAllByCompany(@AuthenticationPrincipal CustomUser customUser,
                                                                                       @RequestParam(name = "page", defaultValue = "0") int page,
                                                                                       @RequestParam(name = "size", defaultValue = "10") int size
                                                                                        ) {
        String comId = customUser.getComId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("boardNo").descending());
        Page<BoardResAllDto> boards = boardService.selectBoardListWithPagingForAllByCompany(pageable,comId);

        return ResponseEntity.ok().body(new ResponseDto<Page<BoardResAllDto>>(HttpStatus.OK, "조회 성공", boards));
    }

    //특정 게시판(Board)에 대한 댓글(Commnet) 목록을 조회
    @GetMapping("/boards/{boardNo}/comments")
    public ResponseEntity<ResponseDto<List<CommentDto>>> getBoardById(@PathVariable(name="boardNo") Long boardNo) {

        return ResponseEntity.ok().body(new ResponseDto<List<CommentDto>>(HttpStatus.OK, "댓글 조회 성공", boardService.getCommentByBoardNo(boardNo)));
    }

    //게시글 삭제
    @DeleteMapping("/board/{boardNo}")
    public ResponseEntity<ResponseDto> delete(@PathVariable(name="boardNo") Long boardNo) {
        boardService.deleteBoard(boardNo);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto(HttpStatus.OK, "게시판 삭제 성공", null));

    }

    //게시글 변경
    @PutMapping("/board/{boardNo}")
    public ResponseEntity<String> update(@PathVariable(name="boardNo") Long boardNo, @RequestBody @Valid BoardReqDto dto) {

        boardService.updateBoard(boardNo, dto);
        return ResponseEntity.ok("상품이 수정되었습니다.");
    }

    // 게시글 작성
    @PostMapping(value ="/board", consumes = {"multipart/form-data"})
    public ResponseEntity<ResponseDto<BoardResAllDto>> regitBoard(@ModelAttribute BoardReqDto dto, @AuthenticationPrincipal CustomUser customUser) {

        dto.setComId(customUser.getComId());
        dto.setEmpId(customUser.getUsername());
        dto.setRating(0);
        dto.setIsDeleted(false);
        System.out.println("dto : " + dto);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<BoardResAllDto>(HttpStatus.OK, "공지사항 등록 성공", boardService.registBoard(dto)));

    }

    //자유 게시글 상세조회  --- 일련번호(key로 조회)
    @GetMapping("/board/{boardNo}")
    public ResponseEntity<ResponseDto<BoardResAllDto>> detail(@PathVariable(name="boardNo") Long boardNo) {
        return ResponseEntity.ok().body(new ResponseDto<BoardResAllDto>(HttpStatus.OK, "조회 성공", boardService.detailBoard(boardNo)));
    }

    @GetMapping("/boards")
    public ResponseEntity<ResponseDto<Page<BoardListItemResDto>>> search(@AuthenticationPrincipal CustomUser customUser,
                                                                         @RequestParam(name = "page", defaultValue = "0") int page,
                                                                         @RequestParam(name = "size", defaultValue = "10") int size,
                                                                         @RequestParam(name = "type", required = false) String type,
                                                                         @RequestParam(name = "keyword", required = false) String keyword,
                                                                         @RequestParam(name = "catCode", required = false) String catCode,
                                                                         @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                         @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String comId = customUser.getComId();
        System.out.println("====== Controller =====");
        System.out.println("type : " + type + ", keyword : " + keyword + ", from : " + from + ", to : " + to);
        System.out.println("page : " + page + ", size : " + size);
        System.out.println("comId : " + comId);

        if (type != null) type = type.trim();
        if (keyword != null) keyword = keyword.trim();
        if (catCode != null) catCode = catCode.trim();
        Pageable pageable = PageRequest.of(page, size, Sort.by("boardNo").descending());

        Page<BoardListItemResDto> result = boardService.searchBoards(comId, type, keyword, catCode, from, to, pageable);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "게시판 조회 성공", result));
    }


    // 각 회사 별 조회수 top 10 게시판 목록 조회
    @GetMapping("/boards/top-view")
    public ResponseEntity<ResponseDto<List<BoardListItemResDto>>> top10ByViewsAll(
            @AuthenticationPrincipal CustomUser user
    ) {
        List<BoardListItemResDto> list = boardService.getTop10ByViews(user);

        return ResponseEntity
                .ok(new ResponseDto<>(HttpStatus.OK, "게시판 조회수 top10 조회 성공", list));
    }

}

