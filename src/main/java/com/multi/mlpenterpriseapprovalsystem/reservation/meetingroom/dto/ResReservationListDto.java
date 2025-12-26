package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 회의실 예약 목록 조회를 위한 DTO
 *
 * 예약 목록 화면에서 여러 예약을 간단히 표시하기 위해
 * 필요한 최소 정보만 포함한다.
 *
 * @author : 송현님
 * @filename : ReservationListDto
 * @since : 2025-12-22 오후 3:21 월요일
 */

@Getter
@AllArgsConstructor
public class ResReservationListDto {
    private Long resvNo;
    private Long roomNo;
    private String roomName;
    private LocalDate resvDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String resvEmpId;
    private String reservedByName;
}
