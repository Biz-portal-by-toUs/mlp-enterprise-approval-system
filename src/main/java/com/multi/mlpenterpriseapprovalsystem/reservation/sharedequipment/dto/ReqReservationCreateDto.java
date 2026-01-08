package com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : MeetingRoomReservationCreateDto
 * @since : 2025-12-24 오후 1:19 수요일
 */

@Getter
public class ReqReservationCreateDto {

    private Long eqNo;
    private LocalDate resvDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purp;

}
