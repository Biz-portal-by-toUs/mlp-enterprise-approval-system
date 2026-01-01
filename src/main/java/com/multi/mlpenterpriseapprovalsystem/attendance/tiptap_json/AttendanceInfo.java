package com.multi.mlpenterpriseapprovalsystem.attendance.tiptap_json;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 근태문서에 json으로 저장되어있는 정보를 파싱해 저장할 객체
 * 근태 시작일, 종료일, 대직자 사번, 대상 근태 식별자
 *
 * @author : 이지헌
 * @filename : AttendanceInfo
 * @since : 25. 12. 30. 화요일
 */
@Getter
@AllArgsConstructor
public class AttendanceInfo {
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String delegateEmpId;
    private Long targetAtteNo;
}
