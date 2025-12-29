package com.multi.mlpenterpriseapprovalsystem.common.jwt.service;

import com.multi.mlpenterpriseapprovalsystem.auth.domain.RefreshToken;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.auth.repository.RefreshTokenRepository;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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
        String accessToken = tokenProvider.createAccessToken(
                user.getSubjectId(),
                user.getSubjectType(),
                user.getComId(),
                user.getUsername(),
                roles
        );

        setAccessCookie(response, accessToken);

        return ResTokenDto.builder()
                .accessToken(accessToken)
                .expiresInSeconds(tokenProvider.getAccessExpSeconds())
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
            if (!tokenProvider.validateToken(existing.getToken())) {
                existing.revoke();
                String newRefresh = createRefreshToken(subjectId, subjectType, comId);
                saveRefresh(subjectType, subjectId, newRefresh);
                return newRefresh;
            }

            return existing.getToken();
        }

        // 기존 토큰 없음 → 신규 발급
        String newRefresh = createRefreshToken(subjectId, subjectType, comId);

        if (!tokenProvider.validateToken(newRefresh)) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        saveRefresh(subjectType, subjectId, newRefresh);
        return newRefresh;
    }

    private String createRefreshToken(Long subjectId, TokenSubjectType subjectType, String comId) {
        return tokenProvider.createRefreshToken(subjectId, subjectType, comId);
    }

    private void saveRefresh(TokenSubjectType type, Long subjectId, String refreshToken) {
        LocalDateTime now = LocalDateTime.now();

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .subjectType(type)
                        .subjectId(subjectId)
                        .token(refreshToken)
                        .expiredAt(now.plusSeconds(tokenProvider.getRefreshExpSeconds()))
                        .createdAt(now)
                        .build()
        );
    }

    private void setAccessCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(tokenProvider.getAccessExpSeconds())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(tokenProvider.getRefreshExpSeconds())
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
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        try {
            // 1) refreshToken 쿠키가 없으면 = 이미 로그아웃 상태로 보고 UNAUTHORIZED
            String refreshToken = extractCookie(request, "refreshToken")
                    .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

            // 2) RT 서명/만료 검증
            if (!tokenProvider.validateToken(refreshToken)) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }

            // 3) RT claims에서 사용자 식별값 추출
            Claims rtClaims = tokenProvider.parseClaims(refreshToken);

            Long subjectId = tokenProvider.getSubjectId(rtClaims.getSubject());
            TokenSubjectType subjectType = tokenProvider.getSubjectType(rtClaims.getSubject());

            // 4) DB에서 유효 RT들 revoke
            var stored = refreshTokenRepository
                    .findAllBySubjectTypeAndSubjectIdAndRevokedFalse(subjectType, subjectId);

            if (stored.isEmpty()) {
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }

            stored.forEach(RefreshToken::revoke);

        } finally {
            // ✅ 성공/실패 상관없이 쿠키는 무조건 삭제
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
        clearCookie(response, "accessToken");
    }

    // =========================================================
    // ✅ 여기부터: AccessToken 재발급 (RefreshToken 기반)
    // =========================================================
    /**
     * ✅ AccessToken 재발급 (새 AccessToken만 발급)
     * - 만료된 accessToken(Authorization 헤더)에서 subject 정보 추출
     * - 쿠키의 refreshToken 검증 + DB의 최신 revoked=false 토큰과 일치 검증
     */
    public ResTokenDto refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = extractCookie(request, "refreshToken")
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // 1) 클라이언트 RT 서명/만료 검증
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 2) RT에서 Claims 추출 (RT는 유효하니 그대로 파싱하면 됨)
        Claims rtClaims = tokenProvider.parseClaims(refreshToken);

        // ✅ 여기서부터: AT 헤더 없이 RT claims로 subject 정보 추출
        Long subjectId = tokenProvider.getSubjectId(rtClaims.getSubject());
        TokenSubjectType subjectType = tokenProvider.getSubjectType(rtClaims.getSubject());
        String comId = tokenProvider.getComId(rtClaims.getSubject());
        String username = tokenProvider.getUsername(rtClaims.getSubject());
        List<String> roles = tokenProvider.getRoles(rtClaims.getSubject());

        // 3) DB에서 현재 유효 RT(최신 revoked=false) 조회
        RefreshToken dbRT = refreshTokenRepository
                .findTopBySubjectTypeAndSubjectIdAndRevokedFalseOrderByRefNoDesc(subjectType, subjectId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

        // 3-1) DB 만료/폐기 체크
        if (dbRT.isRevoked() || LocalDateTime.now().isAfter(dbRT.getExpiredAt())) {
            dbRT.revoke();
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3-2) DB의 RT와 클라이언트 RT 일치 체크
        if (!dbRT.getToken().equals(refreshToken)) {
            dbRT.revoke();
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 4) 새 AccessToken 발급 + 쿠키 세팅
        String newAccessToken = tokenProvider.createAccessToken(
                subjectId, subjectType, comId, username, roles
        );
        setAccessCookie(response, newAccessToken);

        // ✅ 쿠키만 쓸 거면 accessToken을 바디로 굳이 안 내려도 됨
        // (호환/디버깅용으로 남겨도 되고, 없애고 싶으면 null로)
        return ResTokenDto.builder()
                .accessToken(null) // 또는 newAccessToken (원하면 유지)
                .expiresInSeconds(tokenProvider.getAccessExpSeconds())
                .subjectType(subjectType.name())
                .role(roles.isEmpty() ? null : roles.get(0))
                .build();
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();

        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

}