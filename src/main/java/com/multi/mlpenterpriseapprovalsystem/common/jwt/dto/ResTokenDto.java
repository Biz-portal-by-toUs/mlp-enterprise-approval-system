package com.multi.mlpenterpriseapprovalsystem.common.jwt.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * token response : 로그인/토큰 재발금(refresh) 응답
 *
 * @author : 권지영
 * @filename : TokenResponse
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@Builder
public class ResTokenDto {

    private String accessToken;
    // access token 만료까지 남은 시간(초)
    private Long expiresInSeconds;

    // "COMPANY" / "EMPLOYEE"
    private String subjectType;

    // 예: "ROLE_SYS_ADMIN", "ROLE_EMPLOYEE"
    private String role;
}