package com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공유 설비 정보를 클라이언트에 전달하기 위한 Response DTO.
 *
 * 공유 설비 조회 결과를 화면 또는 API 응답으로 제공하기 위해 사용된다.
 * 비즈니스 로직이나 데이터 처리 기능은 포함하지 않는다.
 *
 * @author : 송현님
 * @filename : ResSharedEquipmentDto
 * @since : 2025-12-21 오후 11:34 일요일
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResSharedEquipmentDto {

    private Long eqNo;          // 공유설비 식별자 (PK)
    private String comId;       // 회사 코드
    private String eqName;      // 설비명
    private String eqId;        // 설비번호
    private String modelName;   // 모델명
    private String imageUrl;    // 이미지 URL
    private String location;    // 보관 위치

}
