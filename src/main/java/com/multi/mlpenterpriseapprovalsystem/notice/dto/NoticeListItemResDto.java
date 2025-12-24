package com.multi.mlpenterpriseapprovalsystem.notice.dto;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : ApiResponse
 * @since : 2025-12-22 월요일
 */
public record NoticeListItemResDto(
        Long noticeNo,
        String title,
        String empId,
        Integer rating,
        LocalDateTime createdAt
) {}
