package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : MeetingRoomReservationCreateDto
 * @since : 2025-12-24 오후 1:19 수요일
 */

@Getter
public class ReqReservationCreateDto {

    private Long roomNo;
    private LocalDate resvDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purp;

    private List<String> attendeeIds;
}
