package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import lombok.*;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempResDepartmentDto
 * @since : 25. 12. 20. 토요일
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TempResDepartmentDto {
    private Long depNo;
    private String depId;
    private String depName;
    private String comId;

    public static TempResDepartmentDto toDto(Department department) {
        return TempResDepartmentDto.builder()
                .depNo(department.getDepNo())
                .depId(department.getDepId())
                .depName(department.getDepName())
                .comId(department.getCompany().getComId())
                .build();
    }
}
