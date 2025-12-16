package com.multi.mlpenterpriseapprovalsystem.board.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardCat
 * @since : 2025-12-15 월요일
 */
// 게시판 카테고리
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_cat")
public class BoardCat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardCatNo;

    @Column(unique = true, length = 1)
    private String catCode;

    private String catDescript;
}
