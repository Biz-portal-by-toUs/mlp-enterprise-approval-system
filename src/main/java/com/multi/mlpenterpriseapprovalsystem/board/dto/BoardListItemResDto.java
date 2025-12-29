package com.multi.mlpenterpriseapprovalsystem.board.dto;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : ApiResponse
 * @since : 2025-12-22 월요일
 */
public record BoardListItemResDto(
        Long boardNo,
        String title,
        String empId,
        String empName,
        String depName,
        Integer rating,
        LocalDateTime createdAt
) {}
