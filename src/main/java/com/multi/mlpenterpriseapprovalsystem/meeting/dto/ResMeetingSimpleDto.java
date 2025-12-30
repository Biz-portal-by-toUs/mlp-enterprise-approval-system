package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 회의 조회 list 에 들어갈 회의 상세 내용 resDto
 *
 * @author : 김승기
 * @filename : ResMeetingSimpleDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@Builder
public class ResMeetingSimpleDto {
    private Long meetNo;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String writerEmpId;
    private String writerName;
    private int participantCount;
}