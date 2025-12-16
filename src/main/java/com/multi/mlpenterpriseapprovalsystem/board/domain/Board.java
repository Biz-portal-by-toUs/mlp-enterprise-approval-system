package com.multi.mlpenterpriseapprovalsystem.board.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : Board
 * @since : 2025-12-15 월요일
 */
// 자유게시판
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board")
public class Board extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "comId")
    private Company company;

    private Boolean isDeleted;
    private String title;

    @Column(columnDefinition = "json")
    private String contents;

    private String catCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "empId")
    private Employee writer;

    private Integer rating;
}
