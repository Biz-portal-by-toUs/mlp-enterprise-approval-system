package com.multi.mlpenterpriseapprovalsystem.auth.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.service.AuthService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyLoginDto;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanySignupDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqEmployeeLoginDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


/**
 * 회원가입, 로그인, 로그아웃 부분 컨트롤러
 *
 * @author : 권지영
 * @filename : AuthController
 * @since : 2025. 12. 17. 수요일
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/companies/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Void>> signUpCompany(
            @ModelAttribute ReqCompanySignupDto reqCompanySignupDto,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        ResponseDto<Void> response = authService.signUpCompany(reqCompanySignupDto, logo);

        return ResponseEntity
                .status(response.getStatus())
                .body(response);
    }

    @PostMapping("/companies/login")
    public ResponseEntity<ResponseDto<ResTokenDto>> loginCompany(@RequestBody ReqCompanyLoginDto reqCompanyLoginDto, HttpServletResponse response) {
        ResTokenDto token = authService.loginCompany(reqCompanyLoginDto, response);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "로그인 성공", token));
    }

    @PostMapping("/employee/login")
    public ResponseEntity<ResponseDto<ResTokenDto>> loginEmployee(@RequestBody ReqEmployeeLoginDto reqEmployeeLoginDto, HttpServletResponse response) {
        ResTokenDto token = authService.loginEmployee(reqEmployeeLoginDto, response);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "로그인 성공", token));
    }
}
