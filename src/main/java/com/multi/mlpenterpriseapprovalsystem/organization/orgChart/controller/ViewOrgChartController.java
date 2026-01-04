package com.multi.mlpenterpriseapprovalsystem.organization.orgChart.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 조직도 뷰 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewOrgChartController
 * @since : 2026. 1. 4. 일요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/org-chart")
public class ViewOrgChartController {

    @GetMapping
    public String orgChartPage() {
        return "organization/org-chart";
    }
}
