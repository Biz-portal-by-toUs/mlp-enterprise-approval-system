package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 사원 인증 요청 Dto
 *
 * @author : 권지영
 * @filename : ReqEmployeeIdentityVerifyDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@Setter
public class ReqEmployeeIdentityVerifyDto {

    @NotBlank
    @Size(max = 30)
    private String empName;

    @NotBlank
    @Size(max = 20)
    private String empId;

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;
}
