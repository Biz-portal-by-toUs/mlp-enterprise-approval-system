package com.multi.mlpenterpriseapprovalsystem.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 회사 인증 dto
 *
 * @author : 권지영
 * @filename : ReqCompanyIdentityVerifyDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@Setter
public class ReqCompanyIdentityVerifyDto {

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;
}
