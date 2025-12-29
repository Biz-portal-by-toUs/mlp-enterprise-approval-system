package com.multi.mlpenterpriseapprovalsystem.meeting.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 부서 여러개 추가받을 resDto
 *
 * @author : 김승기
 * @filename : ResMeetingDepartmentDto
 * @since : 2025. 12. 28. 일요일
 */
@Getter
@Builder
public class ResMeetingDepartmentDto {
    private Long depNo;
    private String depName;
}
