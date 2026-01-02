package com.multi.mlpenterpriseapprovalsystem.organization.orgChart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 조직도 부서 반환 dto
 *
 * @author : 권지영
 * @filename : ResOrgDepartmentDto
 * @since : 2026. 1. 2. 금요일
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResOrgDepartmentDto {
    private Long depNo;
    private String depId;
    private String depName;
    private Long empCount;
    private List<ResOrgEmployeeDto> employees;
}
