package com.multi.mlpenterpriseapprovalsystem.reservation.sharedequipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : ReqSharedEquipmentDto
 * @since : 2025-12-21 오후 11:33 일요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqSharedEquipmentDto {

    @NotBlank(message = "설비명은 필수입니다.")
    @Size(max = 50, message = "설비명은 20자 이내여야 합니다.")
    private String eqName;          // 설비명

    @Size(max = 50, message = "설비번호는 20자 이내여야 합니다.")
    private String eqId;            // 설비번호 (선택)

    @Size(max = 50, message = "모델명은 20자 이내여야 합니다.")
    private String modelName;       // 모델명

    @Size(max = 255)
    private String imageUrl;        // 이미지 URL

    @Size(max = 50, message = "보관 위치는 50자 이내여야 합니다.")
    private String location;        // 보관 위치
}
