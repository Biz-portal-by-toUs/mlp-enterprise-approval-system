package com.multi.mlpenterpriseapprovalsystem.organization.positions.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 등록, 수정 요청 dto
 *
 * @author : 권지영
 * @filename : ReqPositionsDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@NoArgsConstructor
public class ReqPositionsDto {

    @NotBlank
    @Size(max = 10)
    String posName;   // 직급 이름

    @NotNull
    @Min(1)          // 1부터 시작하게 할 거면
    @Max(50)
    Integer posOrder;
}
