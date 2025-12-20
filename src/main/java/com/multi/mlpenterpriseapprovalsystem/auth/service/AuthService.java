package com.multi.mlpenterpriseapprovalsystem.auth.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.api.nts.service.BusinessVerificationService;
import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.service.TokenService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.Service.StorageService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.StoredFile;
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
public class AuthService {

    private final CompanyUserDetailService companyUserDetailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmployeeUserDetailService employeeUserDetailService;
    private final CompanyRepository companyRepository;
    private final StorageService storageService;
    private final SubscriptionRepository subscriptionRepository;
    private final BusinessVerificationService businessVerificationService;

    public boolean isRegisteredBusiness(String brn) {
        if (!businessVerificationService.isRegisteredBusiness(brn)) {
            throw new CustomException(ErrorCode.INVALID_BRN);
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

        // 3) 비밀번호 암호화
        String encodedPwd = passwordEncoder.encode(reqCompanySignupDto.getPwd());

        // 2) 로고 저장(있으면 저장하고 imgUrl/path 세팅)
        String imgUrl = null;
        String path = null;
        if (logo != null && !logo.isEmpty()) {
            StoredFile stored = storageService.store(logo, "company-logo");
            imgUrl = stored.getUrl();  // /uploads/company-logo/xxx.png
            path = stored.getPath();   // /Users/.../bizportal/uploads/company-logo/xxx.png
        }

        // 3) sub_no=1 연결 (회원가입 시 기본 요금제)
        Subscription basic = subscriptionRepository.findById((long)1)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 4) Company 생성 + 저장
        Company company = Company.createForSignup(
                reqCompanySignupDto.getComId(),
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
        return tokenService.createToken(user, response);
    }


    public boolean checkComId(String comId) {
        return companyRepository.existsByComId(comId);
    }
}
