package com.multi.mlpenterpriseapprovalsystem.reservation.my.dto;

import groovy.lang.DelegatesTo;
import lombok.*;

import java.time.LocalDate;

/**
 * 내 예약 조회 응답 DTO
 *
 * "내 예약 조회" 화면에서 한 줄(row)에 표시할 데이터를 담는 DTO다.
 * 회의실/법인차량/공유설비처럼 예약 대상이 달라도 화면에서는 동일한 형태로 보여주기 때문에
 * 서로 다른 예약 엔티티들을 공통 포맷으로 통합해서 내려주기 위해 사용한다.
 *
 * @author : 송현님
 * @filename : ResMyReservationDto
 * @since : 2025-12-28 오후 10:30 일요일
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResMyReservationDto {
    private String target;     // 예약대상 (회의실/법인차량/공유설비 등)
    private LocalDate date;    // 예약일자
    private String time;  // 9:00~10:00
    private String purpose;    // 사용 목적
    private String location;      // 장소
    private String status;     // 사용 예정/사용 완료 등

    private String domain;    // (MEETING_ROOM / CORPORATE_CAR / SHARED_EQUIPMENT)
    private Long resvNo;      // 예약 PK
}

