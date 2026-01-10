package com.multi.mlpenterpriseapprovalsystem.search.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.search.infra.ElasticsearchGateway;
import com.multi.mlpenterpriseapprovalsystem.search.web.dto.MenuConfig;
import com.multi.mlpenterpriseapprovalsystem.search.web.dto.SearchGroupedResponse;
import com.multi.mlpenterpriseapprovalsystem.search.web.dto.SearchPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchController
 * @since : 2026. 1. 9. 금요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/search")
public class SearchController {

    private final ElasticsearchGateway es;
    private final EmployeeRepository employeeRepository;

    @GetMapping("/menu")
    public ResponseEntity<ResponseDto<MenuConfig>> getMenu(@AuthenticationPrincipal CustomUser user) throws IOException {
        String comId = user.getComId();
        MenuConfig cfg = es.getMenuConfig().orElseGet(this::defaultMenu);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "검색 메뉴 조회 성공", cfg));
    }

    @GetMapping
    public ResponseEntity<ResponseDto<SearchGroupedResponse>> search(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(name = "q") String q
    ) throws IOException {

        String comId = user.getComId();
        String empId=user.getUsername();
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));


        List<String> acl = List.of(
                "COMP:" + comId,
                "EMP:" + user.getUsername(),
                "DEP:" + employee.getDepartment().getDepNo()
        );

        MenuConfig cfg = es.getMenuConfig().orElseGet(this::defaultMenu);

        SearchGroupedResponse res = es.searchGrouped(comId, q, acl, cfg);
        res.setQuery(q);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "통합검색 성공", res));
    }

    @GetMapping("/page")
    public ResponseEntity<ResponseDto<SearchPageResponse>> searchPage(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "types") List<String> types,
            @RequestParam(name = "scope", required = false) String scope, // 일정일 때만
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) throws IOException {

        String comId = user.getComId();
        String empId = user.getUsername();
        Employee employee = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        List<String> acl = List.of(
                "COMP:" + comId,
                "EMP:" + empId,
                "DEP:" + employee.getDepartment().getDepNo()
        );

        SearchPageResponse res = es.searchPage(comId, q, acl, types, scope, page, size);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "검색(페이징) 성공", res));
    }

    private MenuConfig defaultMenu() {
        MenuConfig cfg = new MenuConfig();
        cfg.setMenus(List.of(
                new MenuConfig.Menu("A", "공지사항", List.of("NOTICE"), 5, 1),
                new MenuConfig.Menu("B", "게시판",   List.of("BOARD"),  5, 2),
                new MenuConfig.Menu("C", "일정",     List.of("SCHEDULE"), 5, 3),
                new MenuConfig.Menu("D", "전자결재", List.of("APPROVAL"), 5, 4),
                new MenuConfig.Menu("E", "회의",     List.of("MEETING"), 5, 5)
        ));
        return cfg;
    }
}