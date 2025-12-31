package com.multi.mlpenterpriseapprovalsystem.auth.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.ReqChangeMyPasswordDto;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.ResChangePasswordDto;
import com.multi.mlpenterpriseapprovalsystem.auth.service.PasswordService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
