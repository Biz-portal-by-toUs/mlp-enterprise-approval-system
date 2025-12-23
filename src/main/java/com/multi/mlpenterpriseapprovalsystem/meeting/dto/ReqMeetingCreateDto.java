package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회의 생성 reqDto
 *
 * @author : 김승기
 * @filename : ReqMeetingCreateDto
 * @since : 2025. 12. 22. 월요일
 */

@Getter
public class ReqMeetingCreateDto {

    @NotBlank
    private String title;

    @NotNull
    private LocalDateTime startedAt;

    @NotNull
    private Boolean status;

    private List<Long> depNos;

    private List<String> participantEmpIds;
}