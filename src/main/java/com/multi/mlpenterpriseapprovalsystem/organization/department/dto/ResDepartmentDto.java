package com.multi.mlpenterpriseapprovalsystem.organization.department.dto;

import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ResDepartmentDto {

    private Long depNo;
    private String depId;
    private String depName;
    private Long empCount;
    private String comId;

    public static ResDepartmentDto toDto(Department department) {
        return ResDepartmentDto.builder()
                .depNo(department.getDepNo())
                .depId(department.getDepId())
                .depName(department.getDepName())
                .comId(department.getCompany().getComId())
                .build();
    }
}
