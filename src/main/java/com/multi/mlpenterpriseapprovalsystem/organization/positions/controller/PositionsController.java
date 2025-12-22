package com.multi.mlpenterpriseapprovalsystem.organization.positions.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.dto.ResPositionsDto;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.service.PositionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 직급 등록, 조회, 수정, 삭제 관련 컨트롤러
 *
 * @author : 권지영
 * @filename : PositionsController
 * @since : 2025. 12. 22. 월요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/positions")
public class PositionsController {

    private final PositionsService positionsService;

    @PreAuthorize("hasRole('COM_ADMIN')")
    @GetMapping
    public ResponseEntity<ResponseDto<List<ResPositionsDto>>> readDepartment(@AuthenticationPrincipal CustomUser user) {

        List<ResPositionsDto> list = positionsService.getPositions(user.getComId());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "부서 조회에 성공했습니다.", list));
    }
}
