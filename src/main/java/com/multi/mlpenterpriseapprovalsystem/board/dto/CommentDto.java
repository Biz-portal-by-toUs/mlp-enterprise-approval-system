package com.multi.mlpenterpriseapprovalsystem.board.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : CommentReqDto
 * @since : 2025-12-18 목요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentDto {

    private Long commentNo; // 수정 시 필요, 등록 시에는 null 가능
    private String comId;
    private Long boardNo;
    private String contents;
    private String empId;
    private LocalDateTime createdAt;
}
