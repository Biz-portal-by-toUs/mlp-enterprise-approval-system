package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "회의실명은 필수입니다.")
    @Size(max = 20, message = "회의실명은 20자 이내여야 합니다.")
    private String roomName;

    @NotNull(message = "수용 인원은 필수입니다.")
    @Min(value = 1, message = "수용 인원은 1명 이상이어야 합니다.")
    private Integer capacity;

    @NotBlank(message = "위치는 필수입니다.")
    @Size(max = 20)
    private String location;

    private String imageUrl;

    @Size(max = 255)
    private String equipList;

    @Size(max = 255)
    private String note;

}
