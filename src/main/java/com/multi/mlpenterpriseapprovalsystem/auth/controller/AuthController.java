package com.multi.mlpenterpriseapprovalsystem.auth.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.auth.service.AuthService;
import com.multi.mlpenterpriseapprovalsystem.auth.service.PasswordService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.service.TokenService;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyIdentityVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyLoginDto;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanySignupDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqEmployeeIdentityVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqEmployeeLoginDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ResAdminEmployeeCreateDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final TokenService tokenService;
    private final PasswordService passwordService;

    @PostMapping(value = "/companies/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ResAdminEmployeeCreateDto>> signUpCompany(
            @ModelAttribute @Valid ReqCompanySignupDto reqCompanySignupDto,
            @RequestPart(value = "logo", required = false) MultipartFile logo
    ) {
        ResponseDto<ResAdminEmployeeCreateDto> response = authService.signUpCompany(reqCompanySignupDto, logo);

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

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto<>(HttpStatus.OK, "인증에 성공하였습니다.", authService.isRegisteredBusiness(brn)));
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

    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<ResTokenDto>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ResTokenDto token = tokenService.refreshAccessToken(request, response); // (쿠키 기반 버전)
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "AccessToken 재발급 성공", token));
    }

    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        tokenService.logout(request, response);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "로그아웃 성공", null));
    }

    // 회사: 인증(이메일만 확인) -> 200이면 프론트에서 다음 페이지로 이동
    @PostMapping("/verify/company")
    public ResponseEntity<ResponseDto<ResVerifyDto>> verifyCompany(
            @Valid @RequestBody ReqCompanyIdentityVerifyDto req
    ) {
        ResVerifyDto res = passwordService.verifyCompanyIdentity(req);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "회사 인증 성공", res));
    }

    // 사원: 인증(이름+사번+이메일 확인) -> 200이면 다음 페이지로 이동
    @PostMapping("/verify/employee")
    public ResponseEntity<ResponseDto<ResVerifyDto>> verifyEmployee(
            @Valid @RequestBody ReqEmployeeIdentityVerifyDto req
    ) {
        ResVerifyDto res = passwordService.verifyEmployeeIdentity(req);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "사원 인증 성공", res));
    }

    //로그인 안하고 비밀번호 변경
    @PatchMapping("/password")
    public ResponseEntity<ResponseDto<ResChangePasswordDto>> changeMyPassword(
            @Valid @RequestBody ReqChangeMyPasswordDto req
    ) {

        ResChangePasswordDto res = passwordService.changePasswordBeforLogin(req);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "비밀번호 변경 성공", res));
    }

    @GetMapping("/me")
    public ResponseEntity<ResponseDto<ResMeDto>> me(@AuthenticationPrincipal CustomUser user) {
        if (user == null) {
            ResMeDto res = new ResMeDto(false, null, null, null);
            return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "로그인 주체 없음", res));
        }
        ResMeDto res = new ResMeDto(
                true,
                user.getSubjectType(),
                user.getSubjectId(),
                user.getUsername()
        );

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "로그인 주체 찾기 성공", res));
    }

}
