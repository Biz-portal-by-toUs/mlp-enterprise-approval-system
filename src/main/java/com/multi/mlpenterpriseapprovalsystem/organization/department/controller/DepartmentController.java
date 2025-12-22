package com.multi.mlpenterpriseapprovalsystem.organization.department.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.organization.department.dto.ReqDepartmentAddDto;
import com.multi.mlpenterpriseapprovalsystem.organization.department.dto.ResDepartmentDto;
import com.multi.mlpenterpriseapprovalsystem.organization.department.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 부서 등록, 수정, 삭제, 조회 컨트롤러
 *
 * @author : 권지영
 * @filename : DepartmentController
 * @since : 2025. 12. 22. 월요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PreAuthorize("hasRole('COM_ADMIN')")
    @PostMapping
    public ResponseEntity<ResponseDto<Void>> addDepartment(@Valid @RequestBody ReqDepartmentAddDto reqDepartmentAddDto, @AuthenticationPrincipal CustomUser user) {

        departmentService.addDepartment(user.getComId(), reqDepartmentAddDto);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "부서 등록에 성공했습니다.", null));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @GetMapping
    public ResponseEntity<ResponseDto<List<ResDepartmentDto>>> readDepartment(@AuthenticationPrincipal CustomUser user) {

        List<ResDepartmentDto> list = departmentService.getDepartmentsWithEmpCount(user.getComId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "부서 조회에 성공했습니다.", list));
    }


}
