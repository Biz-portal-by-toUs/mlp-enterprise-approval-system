package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 회의실 등록 및 수정 요청을 위한 DTO
 *
 * 회의실 관리 기능에서 사용자가 입력한 데이터를
 * 서버로 전달하기 위해 사용하는 요청(Request) DTO이다.
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
