package com.multi.mlpenterpriseapprovalsystem.common.jwt.service;

import com.multi.mlpenterpriseapprovalsystem.auth.domain.RefreshToken;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.auth.repository.RefreshTokenRepository;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 로그인 요청, token 재발급 요청 시 처리
 *
 * @author : 권지영
 * @filename : TokenService
 * @since : 2025. 12. 17. 수요일
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class TokenService {

    // 운영 https면 true, 로컬 http면 false
    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;

    private final TokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    /**
     * 로그인 성공 시 호출: access + refresh 발급
     * - refresh는 DB에서 기존 토큰 재사용(만료/폐기면 재발급)
     */
    public ResTokenDto createToken(CustomUser user, HttpServletResponse response) {

        List<String> roles = user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        // 1) refresh (DB 확인 후 재사용 or 발급)
        String refreshToken = handleRefreshToken(
                user.getSubjectType(),
                user.getSubjectId(),
                user.getComId()
        );

        setRefreshCookie(response, refreshToken);

        // 2) access 발급
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getSubjectId(),
                user.getSubjectType(),
                user.getComId(),
                user.getUsername(),
                roles
        );

        return ResTokenDto.builder()
                .accessToken(accessToken)
                .expiresInSeconds(jwtTokenProvider.getAccessExpSeconds())
                .subjectType(user.getSubjectType().name())
                .role(roles.isEmpty() ? null : roles.get(0))
                .build();
    }

    /**
     * refresh token 처리 로직 (이전 프로젝트 스타일의 JPA 버전)
     * - (subjectType, subjectId)로 기존 refresh를 찾는다
     * - 있으면: 만료/폐기 확인 후 반환
     * - 없으면: 새로 발급 후 저장
     */
    private String handleRefreshToken(TokenSubjectType subjectType, Long subjectId, String comId) {

        RefreshToken existing = refreshTokenRepository
                .findTopBySubjectTypeAndSubjectIdAndRevokedFalseOrderByRefNoDesc(subjectType, subjectId)
                .orElse(null);

        if (existing != null) {
            LocalDateTime now = LocalDateTime.now();

            // DB 만료/폐기 체크
            if (existing.isRevoked() || now.isAfter(existing.getExpiredAt())) {
                existing.revoke(); // revoked=true (엔티티 메서드 필요)
                // 만료면 새로 발급
                String newRefresh = createRefreshToken(subjectId, subjectType, comId);
                saveRefresh(subjectType, subjectId, newRefresh);
                return newRefresh;
            }

            // JWT 서명/만료 체크도 한 번 더 (선택)
            if (!jwtTokenProvider.validateToken(existing.getToken())) {
                existing.revoke();
                String newRefresh = createRefreshToken(subjectId, subjectType, comId);
                saveRefresh(subjectType, subjectId, newRefresh);
                return newRefresh;
            }

            return existing.getToken();
        }

        // 기존 토큰 없음 → 신규 발급
        String newRefresh = createRefreshToken(subjectId, subjectType, comId);

        if (!jwtTokenProvider.validateToken(newRefresh)) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        saveRefresh(subjectType, subjectId, newRefresh);
        return newRefresh;
    }

    private String createRefreshToken(Long subjectId, TokenSubjectType subjectType, String comId) {
        return jwtTokenProvider.createRefreshToken(subjectId, subjectType, comId);
    }

    private void saveRefresh(TokenSubjectType type, Long subjectId, String refreshToken) {
        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .subjectType(type)
                        .subjectId(subjectId)
                        .token(refreshToken)
                        .expiredAt(now.plusSeconds(jwtTokenProvider.getRefreshExpSeconds()))
                        .createdAt(now)
                        .build()
        );
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtTokenProvider.getRefreshExpSeconds())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String resolveToken(String token) {
        // Bearer 접두어가 있는 경우 제거하고 순수한 토큰 반환
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token; // Bearer 접두어가 없는 경우 그대로 반환
    }

    /**
     * 로그아웃: refresh token 폐기
     * - accessToken에서 subject 추출해서 해당 유저의 refresh들을 revoke 처리하고 싶으면 여기도 확장 가능
     */
    @Transactional
    public void deleteRefreshToken(String accessToken, HttpServletResponse response) {

        try {
            String token = resolveToken(accessToken);
            Long subjectId = tokenProvider.getSubjectId(token);
            TokenSubjectType subjectType = tokenProvider.getSubjectType(token);

            var stored = refreshTokenRepository
                    .findAllBySubjectTypeAndSubjectIdAndRevokedFalse(subjectType, subjectId);

            if (stored.isEmpty()) {
                // ✅ 너가 원한대로 UNAUTHORIZED 던짐
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }

            stored.forEach(RefreshToken::revoke);

        } finally {
            // ✅ 성공/예외(UNAUTHORIZED 포함) 상관없이 쿠키 삭제 헤더는 내려감
            clearAuthCookies(response);
        }
    }

    private void clearCookie(HttpServletResponse response, String cookieName) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .build();

        // 여러 쿠키를 삭제할 수도 있으니 addHeader 사용
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, "refreshToken");
        // accessToken을 쿠키로 쓰는 경우만
        // clearCookie(response, "accessToken");
    }

}