package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import com.multi.mlpenterpriseapprovalsystem.employee.enums.MsgStat;
import lombok.Getter;

/**
 * 채팅 사원 Dto
 *
 * @author : 김승기
 * @filename : ChatEmployeeItemDto
 * @since : 2025. 12. 20. 토요일
 */
@Getter
public class ChatEmployeeItemDto {
    private final Long empNo;
    private final String empId;
    private final String empName;

    private final String depName;
    private final String posName;
    private final Integer posOrder;

    private final String msgStat; // c/m/d/x/h
    private final String atte;    // b/v/c

    private final String email;

    public ChatEmployeeItemDto(Long empNo, String empId, String empName,
                               String depName, String posName, Integer posOrder,
                               MsgStat msgStat, String atte, String email) {
        this.empNo = empNo;
        this.empId = empId;
        this.empName = empName;
        this.depName = depName;
        this.posName = posName;
        this.posOrder = posOrder;
        this.msgStat = (msgStat == null ? null : String.valueOf(msgStat.getCode())); // 또는 msgStat.name()
        this.atte = atte;
        this.email = email;
    }
}
