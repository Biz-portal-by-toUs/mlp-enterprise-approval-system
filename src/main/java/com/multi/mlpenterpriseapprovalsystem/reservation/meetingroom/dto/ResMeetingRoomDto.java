package com.multi.mlpenterpriseapprovalsystem.reservation.meetingroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회의실 정보를 클라이언트에 전달하기 위한 Response DTO.
 *
 * 회의실 조회 결과를 화면 또는 API 응답으로 제공하기 위해 사용된다.
 * 비즈니스 로직이나 데이터 처리 기능은 포함하지 않는다.
 * @author : 송현님
 * @filename : MeetingRoomResponseDto
 * @since : 2025-12-16 오후 3:12 화요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResMeetingRoomDto {

    private Long roomNo;          // 회의실 식별자
    private String comId;         // 회사 코드
    private String roomName;      // 회의실명
    private Integer capacity;     // 수용 인원
    private String location;      // 위치
    private String imageUrl;      // 이미지 URL
    private String equipList;     // 장비 목록
    private String note;          // 특이사항


}
