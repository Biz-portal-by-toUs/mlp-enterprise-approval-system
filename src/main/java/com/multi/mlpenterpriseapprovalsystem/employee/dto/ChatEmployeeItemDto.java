package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 채팅 사원 Dto
 *
 * @author : 김승기
 * @filename : ChatEmployeeItemDto
 * @since : 2025. 12. 20. 토요일
 */
@Getter
@AllArgsConstructor
public class ChatEmployeeItemDto {
    private Long empNo;
    private String empId;
    private String empName;

    private String depName;
    private String posName;
    private Integer posOrder;

    private String msgStat; // c/m/d/x/h
    private String atte;    // b/v/c

    private String email;
}
