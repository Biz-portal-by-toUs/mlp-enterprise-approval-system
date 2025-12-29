package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 공유 설비 예약 상세 정보를 전달하기 위한 DTO
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
    private Long eqNo;
    private String eqName;
    private String eqImageUrl;

    // 예약 정보
    private LocalDate resvDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;

    // 예약자
    private String reservedByName;
    private String reservedByDeptName;

}
