package com.multi.mlpenterpriseapprovalsystem.organization.orgChart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 조직도 조회 사원 검색 반환 dto
 *
 * @author : 권지영
 * @filename : ResOrgEmployeeSearchDto
 * @since : 2026. 1. 2. 금요일
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResOrgEmployeeSearchDto {
    private Long empNo;
    private String empId;
    private String empName;

    private String depId;
    private String depName;

    private String posName;
    private Integer posOrder;

    private String profileUrl;
}
