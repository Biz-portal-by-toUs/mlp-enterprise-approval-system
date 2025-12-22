package com.multi.mlpenterpriseapprovalsystem.board.repository;

import com.multi.mlpenterpriseapprovalsystem.board.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : CommentRepository
 * @since : 2025-12-18 목요일
 */
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // board.boardNo 기준으로 댓글 조회
    List<Comment> findByBoard_BoardNo(Long boardNo);

}
