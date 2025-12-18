package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * employee login dto
 *
 * @author : 권지영
 * @filename : ReqEmployeeLoginDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@Setter
public class ReqEmployeeLoginDto {

    private String empId;
    private String password;
}
