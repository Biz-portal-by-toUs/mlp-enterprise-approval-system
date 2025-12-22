package com.multi.mlpenterpriseapprovalsystem.board.service;

import com.multi.mlpenterpriseapprovalsystem.board.domain.Board;
import com.multi.mlpenterpriseapprovalsystem.board.domain.Comment;
import com.multi.mlpenterpriseapprovalsystem.board.dto.CommentDto;
import com.multi.mlpenterpriseapprovalsystem.board.repository.BoardRepository;
import com.multi.mlpenterpriseapprovalsystem.board.repository.CommentRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : CommentService
 * @since : 2025-12-18 목요일
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    public final CommentRepository commentRepository;
    public final BoardRepository boardRepository;
   // public final CompanyRepository companyRepository;
  //  public final EmployeeRepository employeeRepository;


    //특정 댓글 조회 (1건)
    public CommentDto getCommentById(Long commentId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지않습니다."));

        return CommentDto.builder()
                .commentNo(comment.getCommentNo())
                .comId(comment.getCompany().getComId())
                .boardNo(comment.getBoard().getBoardNo())
                .contents(comment.getContents())
                .empId(comment.getEmployee().getEmpId())
                .createdAt(comment.getCreatedAt())
                .build();

    }

    // 댓글 등록
    @Transactional
    public void registerComment(@Valid CommentDto dto) {
        Board board = boardRepository.findById(dto.getBoardNo()).
                orElseThrow(() -> new RuntimeException("게시판 정보가 없습니다"));


//        Company company = companyRepository.findById(dto.getComId()).
//                orElseThrow(() -> new RuntimeException("회사 정보가 없습니다"));
//
//
//        Employee employee = employeeRepository.findById(dto.getEmpId()).
//                orElseThrow(() -> new RuntimeException("사원 정보가 없습니다"));
        System.out.println("board comment save 전 : " + board.getComments().size());

        Comment comment = Comment.builder()
              //  .company(company)
                .board(board)
                .contents(dto.getContents())
              //  .employee(employee)
                .build();

        //commentRepository.save(comment);
        board.addComment(comment);
        System.out.println("board : " + board.getComments().size());

    }

    //댓글 삭제
    @Transactional
    public void deleteComment(Long commentNo) {

        Comment comment = commentRepository.findById(commentNo)
                .orElseThrow(() -> new IllegalArgumentException("댓글 정보가 없습니다"));

        //commentRepository.deleteById(commentNo);
        Board board = comment.getBoard();
        board.removeComment(comment);
        System.out.println("board : " + board.getComments().size());
    }


}
