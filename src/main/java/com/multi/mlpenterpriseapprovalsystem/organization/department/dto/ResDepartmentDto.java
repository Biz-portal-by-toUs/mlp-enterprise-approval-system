package com.multi.mlpenterpriseapprovalsystem.organization.department.dto;

import lombok.*;

/**
 * 부서 조회 반환 dto
 *
 * @author : 권지영
 * @filename : ResDepartmentDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResDepartmentDto {

    private String depId;
    private String depName;
    private long empCount;

    @Builder
    public ResDepartmentDto(String depId, String depName, long empCount) {
        this.depId = depId;
        this.depName = depName;
        this.empCount = empCount;
    }
}
