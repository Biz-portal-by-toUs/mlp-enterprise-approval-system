package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 회의실 예약 상세 정보를 전달하기 위한 DTO
 *
 * 예약 상세 화면(카드 또는 모달)에 필요한 정보를
 * 한 번에 제공하기 위해 사용된다.
 *
 * @author : 송현님
 * @filename : ReservationDetailDto
 * @since : 2025-12-22 오후 2:59 월요일
 */
public class ResReservationDetailDto {
    private Long resvNo;

    // 회의실
    private Long roomNo;
    private String roomName;
    private String roomImageUrl;

    // 예약 정보
    private LocalDate resvDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;

    // 예약자
    private String reservedByName;
    private String reservedByDeptName;

    // 참석자
    private List<String> attendeeNames;
}
