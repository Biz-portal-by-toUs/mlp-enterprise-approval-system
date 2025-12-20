package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.document.service.TempDepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempDocumentController
 * @since : 25. 12. 20. 토요일
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TempDocumentController {

    private final TempDepartmentService tempDepartmentService;


//    public ResponseEntity<ResponseDto<List<TempResDepartmentDto>>> getAllDepartments(@AuthenticationPrincipal CustomUser customUser,
//                                                                                     @RequestParam(name="d", required = false) String depName) {
//
//        List<TempResDepartmentDto> tempResDepartmentDtos = tempDepartmentService.findAllByDepName(customUser.getComId(), depName);
//        return ResponseEntity.ok().body(new ResponseDto<>(HttpStatus.OK, "success", tempResDepartmentDtos));
//    }



    @GetMapping("/documentFormCategorys")
    public ResponseEntity<?> getMockCategories() {
        // 프론트엔드 fetchDropdowns() 구조에 맞춘 임시 데이터
        return ResponseEntity.ok(Map.of("data", List.of(Map.of("name", "지출결의서"), Map.of("name", "휴가신청서"))));
    }

    @GetMapping("/department")
    public ResponseEntity<?> getMockDepartments() {
        return ResponseEntity.ok(Map.of("data", List.of(Map.of("depName", "인사부"), Map.of("depName", "개발부"))));
    }
}
