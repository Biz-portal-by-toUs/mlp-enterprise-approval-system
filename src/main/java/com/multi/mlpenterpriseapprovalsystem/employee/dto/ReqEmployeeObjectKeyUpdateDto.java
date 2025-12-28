package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사원 사진 Object key 등록 dto
 *
 * @author : 권지영
 * @filename : ReqEmployeeObjectKeyUpdateDto
 * @since : 2025. 12. 28. 일요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReqEmployeeObjectKeyUpdateDto {
    @NotBlank
    private String objectKey;
}
