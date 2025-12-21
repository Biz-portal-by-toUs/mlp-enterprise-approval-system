package com.multi.mlpenterpriseapprovalsystem.reservation.corporate_car.dto;

import lombok.*;

/**
 * 법인 차량 정보를 클라이언트에 전달하기 위한 Response DTO.
 *
 * 법인 차량 조회 결과를 화면 또는 API 응답으로 제공하기 위해 사용된다.
 * 비즈니스 로직이나 데이터 처리 기능은 포함하지 않는다.
 *
 * @author : 송현님
 * @filename : ResCorporateCarDto
 * @since : 2025-12-21 오후 1:15 일요일
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResCorporateCarDto {

    private Long carNo;        // 법인차량 식별자
    private String comId;      // 회사 코드
    private String carName;    // 차량명
    private String plateNo;    // 차량 번호
    private String carType;    // 차종
    private String fuel;       // 연료
    private Integer capacity;  // 수용 인원
    private String imageUrl;   // 이미지 URL
}
