package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import lombok.*;

/**
 * 관리자용 사원 등록 반환 dto
 *
 * @author : 권지영
 * @filename : ResAdminEmployeeCreateDto
 * @since : 2025. 12. 28. 일요일
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ResAdminEmployeeCreateDto {
    private Long empNo;
    private String empId;   // 생성된 사번 (ex. CAA0001)
    private String pwd;     // 초기 비밀번호 (ex. 1234)

    public static ResAdminEmployeeCreateDto of(Long empNo, String empId, String pwd) {
        return ResAdminEmployeeCreateDto.builder()
                .empNo(empNo)
                .empId(empId)
                .pwd(pwd)
                .build();
    }
}
