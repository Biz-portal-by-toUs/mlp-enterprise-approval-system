package com.multi.mlpenterpriseapprovalsystem.board.repository;

import com.multi.mlpenterpriseapprovalsystem.board.domain.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardRepository
 * @since : 2025-12-18 목요일
 */
public interface BoardRepository extends JpaRepository<Board, Long>, JpaSpecificationExecutor<Board> {

    List<Board> findByCompany_ComId(String comId);
    Page<Board> findByCompany_ComIdAndIsDeletedFalse(String comId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Board n set n.rating = coalesce(n.rating, 0) + 1 where n.boardNo = :boardNo")
    int incrementRating(@Param("boardNo") Long boardNo);

}
