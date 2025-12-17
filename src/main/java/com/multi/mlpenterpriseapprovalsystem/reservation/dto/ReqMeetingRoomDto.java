package com.multi.mlpenterpriseapprovalsystem.reservation.dto;

import lombok.*;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : ReqMeetingRoomDto
 * @since : 2025-12-17 오후 1:12 수요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqMeetingRoomDto {
    private String roomName;
    private Integer capacity;
    private String location;
    private String imageUrl;
    private String equipList;
    private String note;

}
