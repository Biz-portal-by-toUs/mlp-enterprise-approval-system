package com.multi.mlpenterpriseapprovalsystem.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메인페이지 대시보드 메일, 결재 관련 요약 반환 dto
 *
 * @author : 권지영
 * @filename : ResDashboardDto
 * @since : 2026. 1. 6. 화요일
 */
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResDashboardDto {

    private int newMail;
    private int newApproval;
}
