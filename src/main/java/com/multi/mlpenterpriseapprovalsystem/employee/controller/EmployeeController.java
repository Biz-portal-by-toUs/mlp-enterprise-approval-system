package com.multi.mlpenterpriseapprovalsystem.employee.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ResChatEmployeeCursorDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ResEmployeeDetailDto;
import com.multi.mlpenterpriseapprovalsystem.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사원 관련 컨트롤러
 *
 * @author : 김승기
 * @filename : EmployeeController
 * @since : 2025. 12. 20. 토요일
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    /**
     * ✅ 무한스크롤(커서) 조회 (이름순 정렬)
     * - 전체: /api/v1/employees?size=20
     * - 검색: /api/v1/employees?keyword=kim&size=20
     * - 다음 페이지: /api/v1/employees?cursor=xxxx&size=20
     */
    @GetMapping
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

    @GetMapping("/me")
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
}
