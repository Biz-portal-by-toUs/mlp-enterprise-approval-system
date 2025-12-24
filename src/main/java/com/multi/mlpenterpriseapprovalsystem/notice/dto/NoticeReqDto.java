package com.multi.mlpenterpriseapprovalsystem.notice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : NoticeReqDto
 * @since : 2025-12-16 화요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeReqDto {

    private Long noticeNo; // 수정 시 필요, 등록 시에는 null 가능
    @NotBlank(message = "회사코드는 필수입니다.")
    private String comId;
    @NotNull
    private Boolean isDeleted;
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    @NotBlank(message = "내용은 필수입니다.")
    private String contents;
    @NotNull
    private Boolean isPopup;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endedAt;
    @NotNull(message = "사원 ID는 필수입니다.")
    private String empId;
    private Integer rating;

    private LocalDateTime createdAt;
    private LocalDateTime updated;

}
