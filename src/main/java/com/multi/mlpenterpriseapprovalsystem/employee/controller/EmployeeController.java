package com.multi.mlpenterpriseapprovalsystem.employee.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.*;
import com.multi.mlpenterpriseapprovalsystem.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사원 관련 컨트롤러
 *
 * @author : 김승기
 * @filename : EmployeeController
 * @since : 2025. 12. 20. 토요일
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    /**
     * ✅ 무한스크롤(커서) 조회 (이름순 정렬)
     * - 전체: /api/v1/employees?size=20
     * - 검색: /api/v1/employees?keyword=kim&size=20
     * - 다음 페이지: /api/v1/employees?cursor=xxxx&size=20
     */
    @GetMapping("/employees")
    public ResponseEntity<ResponseDto<ResChatEmployeeCursorDto>> getEmployeesByCursor(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "excludeMe", defaultValue = "true") boolean excludeMe
    ) {
        String requesterEmpId = user.getUsername();
        ResChatEmployeeCursorDto employee = employeeService.getEmployeesByCursor(requesterEmpId, keyword, cursor, size, excludeMe);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK,"사람 이름 조회 성공",employee));
    }

    @GetMapping("/employees/me")
    public ResponseEntity<ResponseDto<ResEmployeeDetailDto>> getMyInfo(
            @AuthenticationPrincipal CustomUser user
    ) {
        String myEmpId = user.getUsername();
        // 서비스에서 사번으로 내 정보 하나만 가져오는 메서드 호출
        ResEmployeeDetailDto myInfo = employeeService.getEmployeeDetailById(myEmpId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "내 정보 조회 성공", myInfo));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @GetMapping("/admin/employees")
    public ResponseEntity<ResponseDto<List<ResEmployeeListDto>>> getEmployees(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(required = false, name="depNo") Long depNo,
            @RequestParam(required = false, name="posNo") Long posNo,
            @RequestParam(required = false, name="status") String status, // "active", "retired"
            @RequestParam(required = false, name="keyword") String keyword
    ) {
        Boolean isDeleted = null;
        if ("active".equals(status)) isDeleted = false;
        else if ("retired".equals(status)) isDeleted = true;

        List<ResEmployeeListDto> list = employeeService.searchEmployees(user.getComId(), depNo, posNo, isDeleted, keyword);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "사원 목록 조회 성공", list));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @GetMapping("/admin/employees/{empNo}") // ✅ 요청하신 POST 방식
    public ResponseEntity<ResponseDto<ResAdminEmployeeDetailDto>> getEmployeeDetail(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="empNo") Long empNo
    ) {
        ResAdminEmployeeDetailDto dto = employeeService.getEmployeeDetail(user.getComId(), empNo);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "사원 상세 조회 성공", dto));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @PatchMapping("/admin/employees/{empNo}/retire")
    public ResponseEntity<ResponseDto<Void>> retireEmployee(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="empNo") Long empNo
    ) {
        employeeService.retireEmployee(user.getComId(), empNo);

        return ResponseEntity.ok(new ResponseDto<>(
                HttpStatus.OK,
                "퇴사 처리 완료",
                null
        ));
    }

    @PreAuthorize("hasRole('COM_ADMIN')")
    @PostMapping("/admin/employees")
    public ResponseEntity<ResponseDto<ResAdminEmployeeCreateDto>> createEmployee(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody ReqAdminEmployeeCreateDto req
    ) {

        return ResponseEntity.ok(new ResponseDto<>(
                HttpStatus.OK,
                "사원 등록 완료",
                employeeService.createEmployee(user.getComId(), req)
        ));
    }

    @PatchMapping("/admin/employees/{empNo}/object-key")
    public void updateEmployeeObjectKey(@PathVariable(name="empNo") Long empNo,
                                        @Valid @RequestBody ReqEmployeeObjectKeyUpdateDto req,
                                        @AuthenticationPrincipal CustomUser user) {

        String comId = user.getComId(); // 너희 인증에서 comId 꺼내는 방식으로 변경
        employeeService.updateObjectKey(comId, empNo, req.getObjectKey());
    }

    @PatchMapping("/admin/employees/{empNo}")
    public ResponseEntity<ResponseDto<Void>> updateEmployee(
            @AuthenticationPrincipal CustomUser user,
            @PathVariable(name="empNo") Long empNo,
            @RequestBody @Valid ReqAdminEmployeeUpdateDto req
    ){

        String comId = user.getComId();
        employeeService.updateEmployee(comId, empNo, req);

        return ResponseEntity.ok(new ResponseDto<>(
                HttpStatus.OK,
                "사원 등록 완료",
                null
        ));
    }
}
