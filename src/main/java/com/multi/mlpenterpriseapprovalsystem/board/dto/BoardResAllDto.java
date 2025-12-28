package com.multi.mlpenterpriseapprovalsystem.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : BoardResDto
 * @since : 2025-12-18 목요일
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResAllDto {
    private Long boardNo;
    private String compId;
    private boolean isDeleted;
    private String title;
    private String contents;
    private Character catCode;
    private String catDescript;
    private String empId;
    private String empName;
    private String depName;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
