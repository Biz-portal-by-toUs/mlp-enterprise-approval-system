package com.multi.mlpenterpriseapprovalsystem.reservation.corporatecar.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : ReqCorporateCarDto
 * @since : 2025-12-21 오후 1:14 일요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqCorporateCarDto {

    @NotBlank(message = "차량명은 필수입니다.")
    @Size(max = 20, message = "차량명은 20자 이내여야 합니다.")
    private String carName;

    @Size(max = 20, message = "차종은 20자 이내여야 합니다.")
    private String carType;

    @NotBlank(message = "차량 번호는 필수입니다.")
    @Size(max = 20, message = "차량 번호는 20자 이내여야 합니다.")
    private String plateNo;

    @NotNull(message = "수용 인원은 필수입니다.")
    @Min(value = 1, message = "수용 인원은 1명 이상이어야 합니다.")
    private Integer capacity;

    @Size(max = 10, message = "연료 정보는 10자 이내여야 합니다.")
    private String fuel;

    private String imageUrl;

}

