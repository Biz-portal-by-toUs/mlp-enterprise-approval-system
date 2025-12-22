package com.multi.mlpenterpriseapprovalsystem.company.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 회사 로그인 요청 dto
 *
 * @author : 권지영
 * @filename : ReqCompnayLoginDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@Setter
public class ReqCompanyLoginDto {
    private String email;
    private String password;
}
