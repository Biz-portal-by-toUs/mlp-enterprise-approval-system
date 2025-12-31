package com.multi.mlpenterpriseapprovalsystem.schedule.dto;

import com.multi.mlpenterpriseapprovalsystem.schedule.domain.EmpSchedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.domain.Schedule;
import com.multi.mlpenterpriseapprovalsystem.schedule.enums.CalendarScope;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사원 개인일정 반환 dto
 *
 * @author : 권지영
 * @filename : ResEmpScheduleDto
 * @since : 2025. 12. 30. 화요일
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResScheduleDto {

    private CalendarScope scope;
    private Long id;

    private String title;
    private String content;

    private LocalDateTime startAt;
    private LocalDateTime endedAt;

    private String color;

    // scope별 옵션 필드
    private String empId; // PERSONAL일 때 세팅
    private Long depNo; // DEPARTMENT일 때 세팅 (COMPANY면 null)

    public static ResScheduleDto fromPersonal(EmpSchedule s) {
        return ResScheduleDto.builder()
                .scope(CalendarScope.PERSONAL)
                .id(s.getSchNo())
                .title(s.getTitle())
                .content(s.getContent())
                .startAt(s.getStartAt())
                .endedAt(s.getEndedAt())
                .color(s.getColor())
                .empId(s.getEmployee().getEmpId())
                .depNo(null)
                .build();
    }

    public static ResScheduleDto fromSchedule(CalendarScope scope, Schedule s) {
        return ResScheduleDto.builder()
                .scope(scope) // COMPANY or DEPARTMENT
                .id(s.getSchNo())
                .title(s.getTitle())
                .content(s.getContent())
                .startAt(s.getStartAt())
                .endedAt(s.getEndedAt())
                .color(s.getColor())
                .empId(null)
                .depNo(s.getDepartment().getDepNo())
                .build();
    }
}
