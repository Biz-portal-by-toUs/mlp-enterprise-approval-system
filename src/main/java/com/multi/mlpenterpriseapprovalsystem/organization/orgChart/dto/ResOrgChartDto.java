package com.multi.mlpenterpriseapprovalsystem.organization.orgChart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 조직도 반환 dto
 *
 * @author : 권지영
 * @filename : ResOrgChartDto
 * @since : 2026. 1. 2. 금요일
 */
@Getter
@AllArgsConstructor
public class ResOrgChartDto {
    private List<ResOrgDepartmentDto> departments;
}
