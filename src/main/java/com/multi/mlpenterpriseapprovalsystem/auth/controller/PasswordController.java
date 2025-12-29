package com.multi.mlpenterpriseapprovalsystem.auth.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.ReqChangeMyPasswordDto;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.ResChangePasswordDto;
import com.multi.mlpenterpriseapprovalsystem.auth.service.PasswordService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyIdentityVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqEmployeeIdentityVerifyDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 비밀번호 변경 컨트롤러
 *
 * @author : 권지영
 * @filename : PasswordController
 * @since : 2025. 12. 29. 월요일
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class PasswordController {

    private final PasswordService passwordService;


    // 회사: 인증(이메일만 확인) -> 200이면 프론트에서 다음 페이지로 이동
    @PostMapping("/verify/company")
    public ResponseEntity<ResponseDto<Void>> verifyCompany(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody ReqCompanyIdentityVerifyDto req
    ) {
        passwordService.verifyCompanyIdentity(user, req);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "회사 인증 성공", null));
    }

    // 사원: 인증(이름+사번+이메일 확인) -> 200이면 다음 페이지로 이동
    @PostMapping("/verify/employee")
    public ResponseEntity<ResponseDto<Void>> verifyEmployee(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody ReqEmployeeIdentityVerifyDto req
    ) {
        passwordService.verifyEmployeeIdentity(user, req);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "사원 인증 성공", null));
    }

    // 비밀번호 변경(인증 통과 후 페이지에서 호출)
    @PatchMapping("/password")
    public ResponseEntity<ResponseDto<ResChangePasswordDto>> changeMyPassword(
            @AuthenticationPrincipal CustomUser user,
            @Valid @RequestBody ReqChangeMyPasswordDto req
    ) {

        ResChangePasswordDto res = passwordService.changePassword(user, req);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "비밀번호 변경 성공", res));
    }
}
