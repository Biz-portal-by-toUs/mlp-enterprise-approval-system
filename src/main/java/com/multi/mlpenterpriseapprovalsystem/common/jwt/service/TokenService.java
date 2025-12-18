package com.multi.mlpenterpriseapprovalsystem.common.jwt.service;

import com.multi.mlpenterpriseapprovalsystem.auth.domain.RefreshToken;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.auth.repository.RefreshTokenRepository;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * token service
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

    private final TokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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
                .secure(true)          // 로컬 http면 false
                .sameSite("Lax")
                .path("/auth")
                .maxAge(jwtTokenProvider.getRefreshExpSeconds())
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 로그아웃: refresh token 폐기
     * - accessToken에서 subject 추출해서 해당 유저의 refresh들을 revoke 처리하고 싶으면 여기도 확장 가능
     */
    @Transactional
    public void logoutByRefreshToken(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
        stored.revoke();
    }
}