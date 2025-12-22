package com.multi.mlpenterpriseapprovalsystem.organization.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 등록 요청 dto
 *
 * @author : 권지영
 * @filename : ReqDepartmentAddDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@NoArgsConstructor
public class ReqDepartmentDto {

    @NotBlank
    @Size(max = 3)
    String depId; // 부서 코드

    @NotBlank
    @Size(max = 10)
    String depName;   // 부서 이름
}
