package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import lombok.Builder;
import lombok.Data;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempResEmployeeDto
 * @since : 25. 12. 22. 월요일
 */
@Data
@Builder
public class TempResEmployeeDto {
    private Long empNo;
    private String empId;
    private TempResDepartmentDto department;
    private TempResPositionDto position;
    private String empName;
    private String email;
    private String phone;
    private String workPhone;
    private String addr;
    private String atte;
    private TempResEmployeeDto delegate;

    public static TempResEmployeeDto toDto(Employee emp) {
        return TempResEmployeeDto.builder()
                .empNo(emp.getEmpNo())
                .empId(emp.getEmpId())
                .empName(emp.getEmpName())
                .email(emp.getEmail())
                .phone(emp.getPhone())
                .workPhone(emp.getWorkPhone())
                .addr(emp.getAddr())
                .atte(emp.getAtte())
                .build();
    }
}
