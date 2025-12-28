package com.multi.mlpenterpriseapprovalsystem.board.repository;

import com.multi.mlpenterpriseapprovalsystem.board.domain.BoardCat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardCatRepository
 * @since : 2025-12-19 금요일
 */
public interface BoardCatRepository extends JpaRepository<BoardCat, Long> {

    Optional<BoardCat> findByCatCode(Character catCode);
}
