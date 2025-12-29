package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import lombok.*;

/**
 * 사원 목록 반환 dto
 *
 * @author : 권지영
 * @filename : ResEmployeeListDto
 * @since : 2025. 12. 23. 화요일
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResEmployeeListDto {

    private Long empNo;
    private String empId;
    private String empName;
    private String depName;
    private String posName;
    private RoleType role;
    private boolean deleted;
    private String email;

    public ResEmployeeListDto(Long empNo, String empId, String empName, String depName, String posName, RoleType role, boolean deleted, String email) {
        this.empNo = empNo;
        this.empId = empId;
        this.empName = empName;
        this.depName = depName;
        this.posName = posName;
        this.role = role;
        this.deleted = deleted;
        this.email = email;
    }

}
