package com.multi.mlpenterpriseapprovalsystem.organization.orgChart.Service;

import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.department.dto.ResDepartmentDto;
import com.multi.mlpenterpriseapprovalsystem.organization.department.service.DepartmentService;
import com.multi.mlpenterpriseapprovalsystem.organization.orgChart.dto.ResOrgChartDto;
import com.multi.mlpenterpriseapprovalsystem.organization.orgChart.dto.ResOrgDepartmentDto;
import com.multi.mlpenterpriseapprovalsystem.organization.orgChart.dto.ResOrgEmployeeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 조직도 확인 서비스
 *
 * @author : 권지영
 * @filename : OrgChartService
 * @since : 2026. 1. 2. 금요일
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgChartService {

    private final DepartmentService departmentService;
    private final EmployeeRepository employeeRepository;

    public ResOrgChartDto getOrgChart(String comId, String depId) {

        // ✅ 조직도 전용: 부서명순 + empCount 포함
        List<ResDepartmentDto> deps = departmentService.getDepartmentsOrderByName(comId);

        // depId가 있으면 해당 부서만 남김
        if (depId != null && !depId.isBlank()) {
            deps = deps.stream().filter(d -> depId.equals(d.getDepId())).toList();
        }

        // 부서 DTO 생성(부서명순 유지)
        List<ResOrgDepartmentDto> depDtos = deps.stream()
                .map(d -> ResOrgDepartmentDto.builder()
                        .depNo(d.getDepNo())
                        .depId(d.getDepId())
                        .depName(d.getDepName())
                        .empCount(d.getEmpCount())
                        .employees(new ArrayList<>())
                        .build())
                .toList();

        Map<String, ResOrgDepartmentDto> depMap = new LinkedHashMap<>();
        for (ResOrgDepartmentDto d : depDtos) depMap.put(d.getDepId(), d);

        if (depDtos.isEmpty()) return new ResOrgChartDto(List.of());

        // ✅ 사원 조회: 부서명순 + posOrder순
        List<EmployeeRepository.OrgEmployeeRow> rows =
                employeeRepository.findOrgChartRows(comId, depId);

        for (EmployeeRepository.OrgEmployeeRow r : rows) {
            ResOrgDepartmentDto parent = depMap.get(r.getDepId());
            if (parent == null) continue;

            parent.getEmployees().add(ResOrgEmployeeDto.builder()
                    .empNo(r.getEmpNo())
                    .empId(r.getEmpId())
                    .empName(r.getEmpName())
                    .depId(r.getDepId())
                    .depName(r.getDepName())
                    .posName(r.getPosName())
                    .posOrder(r.getPosOrder())
                    .objectKey(r.getObjectKey())
                    .workPhone(r.getWorkPhone())
                    .email(r.getEmail())
                    .build());
        }

        return new ResOrgChartDto(depDtos);
    }
}
