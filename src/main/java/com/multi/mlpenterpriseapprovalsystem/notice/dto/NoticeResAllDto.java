package com.multi.mlpenterpriseapprovalsystem.notice.dto;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : NoticeResAllDto
 * @since : 2025-12-16 화요일
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeResAllDto {
    private Long noticeNo;
    private String compId;
    private boolean isDeleted;
    private String title;
    private String contents;
    private Boolean isPopup;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String empId;
    private Integer rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
