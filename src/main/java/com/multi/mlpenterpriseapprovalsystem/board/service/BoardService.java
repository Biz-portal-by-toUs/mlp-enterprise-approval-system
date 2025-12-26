package com.multi.mlpenterpriseapprovalsystem.board.service;

import com.multi.mlpenterpriseapprovalsystem.board.domain.Board;
import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardCatDto;
import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardReqDto;
import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardResAllDto;
import com.multi.mlpenterpriseapprovalsystem.board.dto.CommentDto;
import com.multi.mlpenterpriseapprovalsystem.board.repository.BoardCatRepository;
import com.multi.mlpenterpriseapprovalsystem.board.repository.BoardRepository;
import com.multi.mlpenterpriseapprovalsystem.board.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardService
 * @since : 2025-12-18 목요일
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCatRepository boardCatRepository;
    private final CommentRepository commentRepository;

    // public final CompanyRepository companyRepository;
    //  public final EmployeeRepository employeeRepository;

    public List<BoardCatDto> getAllCategory() {

        // 기존 방식
        return boardCatRepository.findAll().stream().map(
                        boardCat -> BoardCatDto.builder()
                                .boardCatNo(boardCat.getBoardCatNo())
                                .catCode(boardCat.getCatCode())
                                .catDescript(boardCat.getCatDescript())
                                .build())
                .collect(Collectors.toList());


    }

    public Page<BoardResAllDto> selectBoardListWithPagingForAll(Pageable pageable) {
        Page<Board> boards = boardRepository.findAll(pageable);

        // 기존 방식
        return boards.map( board -> BoardResAllDto.builder()
                .boardNo(board.getBoardNo())
                .compId(board.getCompany().getComId())
                .isDeleted(board.getIsDeleted())
                .title(board.getTitle())
                .contents(board.getContents())
                .catCode(board.getCatCode())
                .rating(board.getRating())
                .empId(board.getEmployee().getEmpId())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build());


    }

    //회사별로 조회시 페이징 처리
    public Page<BoardResAllDto> selectBoardListWithPagingForAllByCompany(Pageable pageable, String comId) {
        Page<Board> boards = boardRepository.findByCompany_ComIdAndIsDeletedFalse(comId, pageable);

        // 기존 방식
        return boards.map( board -> BoardResAllDto.builder()
                .boardNo(board.getBoardNo())
                .compId(board.getCompany().getComId())
                .isDeleted(board.getIsDeleted())
                .title(board.getTitle())
                .contents(board.getContents())
                .catCode(board.getCatCode())
                .rating(board.getRating())
                .empId(board.getEmployee().getEmpId())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build());
    }

    //특정 게시판에 달린 댓글 전체 조회
    public List<CommentDto> getCommentByBoardNo(Long boardNo) {
        Board board = boardRepository.findById(boardNo)
                .orElseThrow(() -> new IllegalArgumentException("게시판이 존재하지 않습니다"));

        return commentRepository.findByBoard_BoardNo(boardNo).stream()
                .map(c -> CommentDto.builder()
                        .commentNo(c.getCommentNo())
                        .comId(c.getCompany().getComId())
                        .boardNo(c.getBoard().getBoardNo())
                        .contents(c.getContents())
                        .empId(c.getEmployee().getEmpId())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

//        return board.getComments().stream()
//                .map(comment -> CommentDto.builder()
//                        .commentNo(comment.getCommentNo())
//                        .comId(comment.getCompany().getComId())
//                        .boardNo(board.getBoardNo())
//                        .contents(comment.getContents())
//                        .empId(comment.getEmployee().getEmpId())
//                        .createdAt(comment.getCreatedAt())
//                        .build())
//                .collect(Collectors.toList());
    }

    //특정게시판 삭제
    @Transactional
    public void deleteBoard(Long boardNo) {

        Board board = boardRepository.findById(boardNo)
                .orElseThrow(() -> new IllegalArgumentException("게시판 정보가 없습니다")); // 내가 해봄
        boardRepository.deleteById(boardNo);
    }

    @Transactional
    public void updateBoard(Long boardNo, BoardReqDto dto) {
        Board board = boardRepository.findById(boardNo)
                .orElseThrow(() -> new IllegalArgumentException("게시판 정보가 없습니다"));

        board.update(dto);
    }

    @Transactional
    public void registBoard(BoardReqDto dto) {
//        Company company = companyRepository.findById(dto.getComId())
//                .orElseThrow(()->new IllegalArgumentException("회사정보가 없습니다 "));
//
//        Employee employee = employeeRepository.findById(dto.getEmpId())
//                .orElseThrow(()->new IllegalArgumentException("사원정보가 없습니다 "));

        Board board = Board.builder()
                .isDeleted(dto.getIsDeleted())
                .title(dto.getTitle())
                .contents(dto.getContents())
                .catCode(dto.getCatCode())
                .rating(dto.getRating())
                .build();

      //  board.assignCompany(company);
       // board.assignEmployee(employee);
        boardRepository.save(board);
    }

    public BoardResAllDto detailBoard(Long boardNo) {

        Board board = boardRepository.findById(boardNo).orElseThrow(() -> new IllegalArgumentException("자유게시판이 존재하지 않습니다"));

        return BoardResAllDto.builder()
                .boardNo(board.getBoardNo())
                .compId(board.getCompany().getComId())
                .isDeleted(board.getIsDeleted())
                .title(board.getTitle())
                .contents(board.getContents())
                .empId(board.getEmployee().getEmpId())
                .createdAt(board.getCreatedAt())
                .build();

    }



}
