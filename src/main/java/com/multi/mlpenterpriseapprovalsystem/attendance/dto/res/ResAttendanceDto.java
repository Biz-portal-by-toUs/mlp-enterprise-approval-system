package com.multi.mlpenterpriseapprovalsystem.attendance.dto.res;

import com.multi.mlpenterpriseapprovalsystem.attendance.domain.Attendance;
import com.multi.mlpenterpriseapprovalsystem.attendance.enums.AtteType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : ResAttendanceDto
 * @since : 25. 12. 31. 수요일
 */
@Builder
@Data
public class ResAttendanceDto {
    private Long atteNo;

    private Long comNo;
    private String comId;

    private Long myEmpNo; // 본인(사원) 식별자
    private String myEmpId; // 본인(사원) 사원번호
    private String myEmpName; // 본인(사원) 이름

    private Long docNo;
    private String docId;
    private String docTitle;

    private AtteType type; // V(휴가), B(출장)
    private Integer day; // 일 수

    private Long delegateEmpNo; // 대직자 식별자
    private String delegateEmpId; // 대직자 사원번호
    private String delegateEmpName; // 대직자 이름

    private LocalDateTime createdAt;
    private LocalDateTime startAt; // 시작일
    private LocalDateTime endAt; // 종료일


    public static ResAttendanceDto toDto(Attendance attendance) {
        ResAttendanceDtoBuilder builder = ResAttendanceDto.builder()
                .atteNo(attendance.getAtteNo())
                .type(attendance.getType())
                .day(attendance.getDay())
                .createdAt(attendance.getCreatedAt())
                .startAt(attendance.getStartAt())
                .endAt(attendance.getEndAt());

        // 회사 정보
        if (attendance.getCompany() != null) {
            builder.comNo(attendance.getCompany().getComNo())
                    .comId(attendance.getCompany().getComId());
        }

        // 사원 정보 (본인)
        if (attendance.getEmployee() != null) {
            builder.myEmpNo(attendance.getEmployee().getEmpNo())
                    .myEmpId(attendance.getEmployee().getEmpId())
                    .myEmpName(attendance.getEmployee().getEmpName());
        }

        // 문서 정보
        if (attendance.getDocument() != null) {
            builder.docNo(attendance.getDocument().getDocNo())
                    .docId(attendance.getDocument().getDocId())
                    .docTitle(attendance.getDocument().getTitle());
        }

        // 대직자 정보 (있을 경우에만)
        if (attendance.getDelegate() != null) {
            builder.delegateEmpNo(attendance.getDelegate().getEmpNo())
                    .delegateEmpId(attendance.getDelegate().getEmpId())
                    .delegateEmpName(attendance.getDelegate().getEmpName());
        }

        return builder.build();
    }
}
