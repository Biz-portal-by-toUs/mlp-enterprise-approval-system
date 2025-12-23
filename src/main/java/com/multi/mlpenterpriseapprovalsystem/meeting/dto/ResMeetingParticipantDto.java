package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 회의 참석자 조회 (resDto)
 *
 * @author : 김승기
 * @filename : ResMeetingParticipantDto
 * @since : 2025. 12. 22. 월요일
 */

@Getter
@Builder
public class ResMeetingParticipantDto {
    private String empId;
    private String empName;
}