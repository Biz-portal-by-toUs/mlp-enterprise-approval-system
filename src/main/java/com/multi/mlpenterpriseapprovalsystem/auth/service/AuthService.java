package com.multi.mlpenterpriseapprovalsystem.auth.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.api.nts.service.BusinessVerificationService;
import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.service.TokenService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.StoredFile;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.StorageService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyLoginDto;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanySignupDto;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqEmployeeLoginDto;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 회원가입, 로그인, 로그아웃 부분 서비스
 *
 * @author : 권지영
 * @filename : AuthService
 * @since : 2025. 12. 17. 수요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final CompanyUserDetailService companyUserDetailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmployeeUserDetailService employeeUserDetailService;
    private final CompanyRepository companyRepository;
    private final StorageService storageService;
    private final SubscriptionRepository subscriptionRepository;
    private final BusinessVerificationService businessVerificationService;

    private static final String DEFAULT_PASSWORD = "1234";

    @Transactional(readOnly = true)
    public boolean isRegisteredBusiness(String brn) {
        if (!businessVerificationService.isRegisteredBusiness(brn)) {
            throw new CustomException(ErrorCode.INVALID_BRN);
        } else if (companyRepository.existsByBrn(brn)) {
            throw new CustomException(ErrorCode.BRN_DUPLICATE);
        }
        return true;
    }

    public ResponseDto<Void> signUpCompany(ReqCompanySignupDto reqCompanySignupDto, MultipartFile logo) {


        // 1) 이메일 중복 체크
        if (companyRepository.existsByEmail(reqCompanySignupDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 2) 회사 코드 중복 체크
        if (companyRepository.existsByComId(reqCompanySignupDto.getComId())) {
            throw new CustomException(ErrorCode.DUPLICATE_COMID);
        }
        // 회사 코드 ToUpperCase
        String comId = reqCompanySignupDto.getComId().trim().toUpperCase();

        // 3) 비밀번호 암호화
        String encodedPwd = passwordEncoder.encode(reqCompanySignupDto.getPwd());

        // 4) 로고 저장(있으면 저장하고 imgUrl/path 세팅)
        String imgUrl = null;
        String path = null;
        if (logo != null && !logo.isEmpty()) {
            StoredFile stored = storageService.store(logo, "company-logo");
            imgUrl = stored.getUrl();  // /uploads/company-logo/xxx.png
            path = stored.getPath();   // /Users/.../bizportal/uploads/company-logo/xxx.png
        }

        // 5) sub_no=1 연결 (회원가입 시 기본 요금제)
        Subscription basic = subscriptionRepository.findById((long) 1)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 6) Company 생성 + 저장
        Company company = Company.createForSignup(
                comId,
                reqCompanySignupDto.getComName(),
                reqCompanySignupDto.getEmail(),
                encodedPwd,
                reqCompanySignupDto.getBrn(),
                reqCompanySignupDto.getAddr(),
                imgUrl,
                path,
                basic,
                RoleType.COM_ADMIN
        );

        companyRepository.save(company);

        return new ResponseDto<>(HttpStatus.CREATED, "회사 회원가입 성공", null);
    }

    public ResTokenDto loginCompany(ReqCompanyLoginDto reqCompanyLoginDto, HttpServletResponse response) {

        // 1) 회사 사용자 조회 (CompanyUserDetailService가 CustomUser를 반환하도록 구현)
        CustomUser user = companyUserDetailService.loadUserByUsername(reqCompanyLoginDto.getEmail());

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(reqCompanyLoginDto.getPassword(), user.getPassword())) {
            // 너희 ErrorCode 쓰는 방식이면 이걸 추천
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }


        // 3) 토큰 발급 + refresh 쿠키 세팅
        return tokenService.createToken(user, response);
    }


    public ResTokenDto loginEmployee(ReqEmployeeLoginDto reqEmployeeLoginDto, HttpServletResponse response) {

        // 1) 회사 사용자 조회 (CompanyUserDetailService가 CustomUser를 반환하도록 구현)
        CustomUser user = employeeUserDetailService.loadUserByUsername(reqEmployeeLoginDto.getEmpId());

        log.info("username>>>>>>>>>>>>>>>>>>>> " + user.getUsername());

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(reqEmployeeLoginDto.getPassword(), user.getPassword())) {
            // 너희 ErrorCode 쓰는 방식이면 이걸 추천
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3) 토큰 발급 + refresh 쿠키 세팅
        ResTokenDto res = tokenService.createToken(user, response);

        boolean mustChange = passwordEncoder.matches(DEFAULT_PASSWORD, user.getPassword());

        return res.toBuilder()
                .mustChangePassword(mustChange)
                .build();
    }


    @Transactional(readOnly = true)
    public boolean checkComId(String comId) {

        // comId 무조건 대문자 처리
        comId = comId.trim().toUpperCase();

        if (companyRepository.existsByComId(comId)) {
            throw new CustomException(ErrorCode.DUPLICATE_COMID);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {

        if (companyRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        return true;
    }


}
