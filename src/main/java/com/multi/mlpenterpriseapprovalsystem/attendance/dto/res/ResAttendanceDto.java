package com.multi.mlpenterpriseapprovalsystem.attendance.dto.res;

import com.multi.mlpenterpriseapprovalsystem.attendance.domain.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 근태 응답 Dto
 *
 * @author : 이지헌
 * @filename : ResAttendanceDto
 * @since : 25. 12. 31. 수요일
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResAttendanceDto {
    private Long atteNo;

    // 회사 정보
    private Long comNo;
    private String comId;

    // 작성자(사원) 정보 - 필드명을 목록 페이지 로직과 통일
    private String writer;        // 사원이름 (JS의 item.writer와 매핑)
    private String writerId;      // 사원번호 (JS의 item.writerId와 매핑)
    private String writerDepName; // 부서이름 (JS의 item.writerDepName과 매핑)

    // 문서 정보
    private Long docNo;
    private String docId;
    private String docTitle;

    // 근태 상세
    private String atteType;      // JS의 item.atteType과 매핑 (V 또는 B)
    private Integer day;          // 일 수

    // 대직자 정보
    private String delegateEmpId;
    private String delegateEmpName;

    private LocalDateTime createdAt;
    private LocalDateTime startAt; // 시작일
    private LocalDateTime endAt;   // 종료일

    public static ResAttendanceDto toDto(Attendance attendance) {
        if (attendance == null) return null;

        ResAttendanceDtoBuilder builder = ResAttendanceDto.builder()
                .atteNo(attendance.getAtteNo())
                .atteType(attendance.getType().name()) // Enum을 String으로 변환 (V, B)
                .day(attendance.getDay())
                .createdAt(attendance.getCreatedAt())
                .startAt(attendance.getStartAt())
                .endAt(attendance.getEndAt());

        // 회사 정보 추출
        if (attendance.getCompany() != null) {
            builder.comNo(attendance.getCompany().getComNo())
                    .comId(attendance.getCompany().getComId());
        }

        // 작성자 및 부서 정보 추출 (핵심!)
        if (attendance.getEmployee() != null) {
            builder.writer(attendance.getEmployee().getEmpName())
                    .writerId(attendance.getEmployee().getEmpId());

            // Employee -> Department 관계 추적
            if (attendance.getEmployee().getDepartment() != null) {
                builder.writerDepName(attendance.getEmployee().getDepartment().getDepName());
            }
        }

        // 문서 정보 추출
        if (attendance.getDocument() != null) {
            builder.docNo(attendance.getDocument().getDocNo())
                    .docId(attendance.getDocument().getDocId())
                    .docTitle(attendance.getDocument().getTitle());
        }

        // 대직자 정보 추출
        if (attendance.getDelegate() != null) {
            builder.delegateEmpId(attendance.getDelegate().getEmpId())
                    .delegateEmpName(attendance.getDelegate().getEmpName());
        }

        return builder.build();
    }
}
