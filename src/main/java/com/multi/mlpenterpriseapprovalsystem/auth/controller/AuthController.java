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

    @PostMapping("/companies/check-comid")
    public ResponseEntity<ResponseDto<Boolean>> checkComId(@RequestParam(name = "comId") String comId) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "사용 가능한 회사 코드 입니다.", authService.checkComId(comId)));
    }

    @PostMapping("/companies/check-email")
    public ResponseEntity<ResponseDto<Boolean>> checkEmail(@RequestParam(name = "email") String email) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "사용 가능한 이메일 입니다.", authService.checkEmail(email)));
    }

    @PostMapping("/companies/verify-brn")
    public ResponseEntity<ResponseDto<Boolean>> verifyBrn(@RequestParam(name = "brn") String brn) {

        if (authService.isRegisteredBusiness(brn)) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new ResponseDto<>(HttpStatus.OK, "인증에 성공하였습니다.", true));
        } else {
            // 200 OK를 보내되 결과값만 false로 주거나, 400 에러를 보낼 수 있습니다.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ResponseDto<>(HttpStatus.BAD_REQUEST, "유효하지 않은 사업자번호입니다.", false));
        }
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
