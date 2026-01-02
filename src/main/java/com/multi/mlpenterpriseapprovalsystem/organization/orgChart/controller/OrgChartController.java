package com.multi.mlpenterpriseapprovalsystem.organization.orgChart.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.organization.orgChart.Service.OrgChartService;
import com.multi.mlpenterpriseapprovalsystem.organization.orgChart.dto.ResOrgChartDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 조직도 확인 컨트롤러
 *
 * @author : 권지영
 * @filename : OrgChartController
 * @since : 2026. 1. 2. 금요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/org-chart")
public class OrgChartController {

    private final OrgChartService orgChartService;

    @GetMapping
    public ResponseEntity<ResponseDto<ResOrgChartDto>> getOrgChart(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(required = false, name="depId") String depId
    ) {
        ResOrgChartDto data = orgChartService.getOrgChart(user.getComId(), depId);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "조직도 조회 성공", data));
    }
}
