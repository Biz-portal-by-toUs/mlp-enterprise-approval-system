package com.multi.mlpenterpriseapprovalsystem.company.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 회사 회원가입 요청 dto
 *
 * @author : 권지영
 * @filename : CompanyReqDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@Setter
public class ReqCompanySignupDto {

    private String comId;
    private String comName;
    private String email;
    private String pwd;
    private String brn;
    private String addr;
    private Integer subNo;
}
