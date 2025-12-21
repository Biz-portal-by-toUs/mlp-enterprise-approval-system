package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 내 정보 조회 resDto
 *
 * @author : 김승기
 * @filename : ResEmployeeDetailDto
 * @since : 2025. 12. 21. 일요일
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ResEmployeeDetailDto {
    private Long empNo;
    private String empId;

    // 회사 정보
    private String comId;
    private String comName;

    // 부서 정보
    private Long depNo;
    private String depName;

    // 직급 정보
    private Long posNo;
    private String posName;

    private String empName;
    private String email;
    private String phone;
    private String workPhone;
    private String gen;
    private LocalDateTime hireDate;
    private String addr;
    private RoleType role;

    private String msgStat;

    // 대직자 정보 (성함 표시용)
    private String delegateEmpId;
    private String delegateName;

    /**
     * Entity -> DTO 변환 정적 메서드
     */
    public static ResEmployeeDetailDto from(Employee employee) {
        return ResEmployeeDetailDto.builder()
                .empNo(employee.getEmpNo())
                .empId(employee.getEmpId())
                .comId(employee.getCompany().getComId())
                .comName(employee.getCompany().getComName())
                .depNo(employee.getDepartment().getDepNo())
                .depName(employee.getDepartment().getDepName())
                .posNo(employee.getPosition().getPosNo())
                .posName(employee.getPosition().getPosName())
                .empName(employee.getEmpName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .workPhone(employee.getWorkPhone())
                .gen(employee.getGen())
                .hireDate(employee.getHireDate())
                .addr(employee.getAddr())
                .role(employee.getRole())
                .msgStat(employee.getMsgStat())
                .delegateEmpId(employee.getDelegate() != null ? employee.getDelegate().getEmpId() : null)
                .delegateName(employee.getDelegate() != null ? employee.getDelegate().getEmpName() : null)
                .build();
    }

}
