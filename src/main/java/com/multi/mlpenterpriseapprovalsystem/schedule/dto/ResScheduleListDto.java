package com.multi.mlpenterpriseapprovalsystem.schedule.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 기간별 일정 리스트 조회
 *
 * @author : 권지영
 * @filename : ResEmpScheduleListDto
 * @since : 2025. 12. 30. 화요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResScheduleListDto {

    private LocalDateTime fromInclusive;
    private LocalDateTime toExclusive;
    private List<ResScheduleDto> items;
}
