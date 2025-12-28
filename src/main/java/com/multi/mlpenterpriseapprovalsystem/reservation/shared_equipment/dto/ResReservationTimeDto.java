package com.multi.mlpenterpriseapprovalsystem.reservation.shared_equipment.dto;

import java.time.LocalDateTime;

/**
 * 공유 설비 예약 타임라인 표시를 위한 DTO
 *
 * 예약의 시작 시간과 종료 시간 정보를 제공하여
 * 공유 설비 예약 타임라인(시간 막대)을 그리는 데 사용된다.
 *
 * @author : 송현님
 * @filename : ReservationTimeDto
 * @since : 2025-12-22 오후 2:59 월요일
 */
public class ResReservationTimeDto {

    private Long resvNo;
    private Long eqNo;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
