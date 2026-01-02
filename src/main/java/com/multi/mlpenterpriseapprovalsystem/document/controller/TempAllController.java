package com.multi.mlpenterpriseapprovalsystem.document.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.TempResDepartmentDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.TempResDocumentFormDto;
import com.multi.mlpenterpriseapprovalsystem.document.dto.res.TempResEmployeeDto;
import com.multi.mlpenterpriseapprovalsystem.document.service.TempAllService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 임시 컨트롤러
 *
 * @author : 이지헌
 * @filename : TempDocumentController
 * @since : 25. 12. 20. 토요일
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/temp")
public class TempAllController {

    private final TempAllService tempAllService;

    // 사원 정보 조회(employee, department, position)
    @GetMapping("/employees/{emp_id}")
    public ResponseEntity<ResponseDto<TempResEmployeeDto>> getEmployeeByEmpId(@PathVariable(name = "emp_id") String empId) {
        TempResEmployeeDto tempResEmployeeDto = tempAllService.getEmployeeByEmpId(empId);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "사원번호로 사원 정보 조회 성공", tempResEmployeeDto));
    }

    // 내 정보 조회
    @GetMapping("/employees/me")
    public ResponseEntity<ResponseDto<TempResEmployeeDto>> getMyEmployee(@AuthenticationPrincipal CustomUser customUser) {
        TempResEmployeeDto tempResEmployeeDto = tempAllService.getEmployeeByEmpId(customUser.getUsername());

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "내 정보 조회 성공", tempResEmployeeDto));
    }

    // 전체 문서양식 조회
    @GetMapping("/documentForms")
    public ResponseEntity<ResponseDto<List<TempResDocumentFormDto>>> getAllDocumentForms(@AuthenticationPrincipal CustomUser customUser) {
        List<TempResDocumentFormDto> tempResDocumentFormDto = tempAllService.getAllDocumentForms(customUser.getComId());

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "전체 문서양식 조회 성공", tempResDocumentFormDto));
    }


    // 문서양식 식별자로 문서양식 및 문서양식 내 카테고리 조회(문서양식 상세 조회)
    @GetMapping("/documentForms/{docfoNo}")
    public ResponseEntity<ResponseDto<TempResDocumentFormDto>> getDocumentFormWithCategory(@PathVariable(name = "docfoNo") Long docfoNo,
                                                                               @AuthenticationPrincipal CustomUser customUser) {

        TempResDocumentFormDto tempResDocumentFormDto = tempAllService.getDocumentFormWithCategory(customUser.getComId(), docfoNo);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 식별자로 문서양식 및 문서양식 내 카테고리 조회 성공", tempResDocumentFormDto));
    }

    // 전체 부서 조회
    @GetMapping("/departments")
    public ResponseEntity<ResponseDto<List<TempResDepartmentDto>>> getDepartments(@AuthenticationPrincipal CustomUser customUser) {

        List<TempResDepartmentDto> tempResDepartmentDtos = tempAllService.getDepartments(customUser.getComId());

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "전체 부서 조회 성공", tempResDepartmentDtos));
    }

    // 문서 양식 내 카테고리 전체 조회(카테고리 이름만 조회. 중복 불가)
    @GetMapping("/documentFormCategoryNames")
    public ResponseEntity<ResponseDto<List<String>>> getDocumentFormCategoryNames(@AuthenticationPrincipal CustomUser customUser) {

        List<String> documentFormCategoryNames =
                tempAllService.getDocumentFormCategoryNames(customUser.getComId());

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "문서양식 내 카테고리 이름 조회 성공", documentFormCategoryNames));
    }
}
