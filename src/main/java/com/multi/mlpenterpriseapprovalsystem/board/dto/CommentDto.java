package com.multi.mlpenterpriseapprovalsystem.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "회사코드는 필수입니다.")
    private String comId;
    @NotNull(message = "게시판 번호는 필수입니다..")
    private Long boardNo;
    @NotBlank(message = "내용은 필수입니다.")
    private String contents;
    @NotNull(message = "사원 ID는 필수입니다.")
    private String empId;

    private LocalDateTime createdAt;
}
