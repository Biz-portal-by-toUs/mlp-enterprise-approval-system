package com.multi.mlpenterpriseapprovalsystem.board.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : BoardCat
 * @since : 2025. 12. 16. 화요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_cat")
public class BoardCat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardCatNo;
    @Column(unique = true, length = 1)
    private Character catCode;
    private String catDescript;
}