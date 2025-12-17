package com.multi.mlpenterpriseapprovalsystem.auth.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.service.TokenService;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyLoginDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * authservice
 *
 * @author : 권지영
 * @filename : AuthService
 * @since : 2025. 12. 17. 수요일
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final CompanyUserDetailService companyUserDetailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public ResTokenDto loginCompany(ReqCompanyLoginDto dto, HttpServletResponse response) {

        // 1) 회사 사용자 조회 (CompanyUserDetailService가 CustomUser를 반환하도록 구현)
        UserDetails userDetails = companyUserDetailService.loadUserByUsername(dto.getEmail());

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(dto.getPassword(), userDetails.getPassword())) {
            // 너희 ErrorCode 쓰는 방식이면 이걸 추천
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3) principal(CustomUser) 추출
        CustomUser user = (CustomUser) userDetails;

        // 4) 토큰 발급 + refresh 쿠키 세팅
        return tokenService.createToken(user, response);
    }
}
