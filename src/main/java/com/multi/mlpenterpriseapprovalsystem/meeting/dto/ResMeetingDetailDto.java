package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회의 상세 조회 resDto
 *
 * @author : 김승기
 * @filename : ResMeetingDetailDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@Builder
public class ResMeetingDetailDto {
    private Long meetNo;
    private String title;
    private String sttText;
    private String aiText;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private boolean status;

    private String recordUrl;
    private String objectKey;


    private String writerEmpId;
    private String writerName;

    private List<ResMeetingParticipantDto> participants;
    private List<ResMeetingDepartmentDto> departments;
}