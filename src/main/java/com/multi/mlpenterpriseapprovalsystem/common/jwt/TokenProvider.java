package com.multi.mlpenterpriseapprovalsystem.common.jwt;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.TokenException;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 토큰 발급/검증/Authentication 생성 Provider
 * - AccessToken: sub=subjectId, subjectType, comId, auth 포함
 * - RefreshToken: sub=subjectId, subjectType, comId 포함 (auth 없음)
 *
 * @author : 권지영
 * @filename : JwtProvider
 * @since : 2025. 12. 17. 수요일
 */
@Component
@Slf4j
public class TokenProvider {

    // ===== Claim Keys =====
    private static final String CLAIM_AUTH = "auth";
    private static final String CLAIM_SUBJECT_TYPE = "subjectType"; // COMPANY / EMPLOYEE
    private static final String CLAIM_COM_ID = "comId";
    private static final String CLAIM_TOKEN_KIND = "tokenKind";     // "A" or "R"

    // ===== Expire =====
    private static final long ACCESS_TOKEN_EXPIRE_TIME_MS = 1000L * 60 * 3;  // 3분
    private static final long REFRESH_TOKEN_EXPIRE_TIME_MS = 1000L * 60 * 5; // 5분 (원하면 늘려)

    private final Key SKEY;
    private final String ISSUER;

    public TokenProvider(JwtProvider jwtProvider) {
        this.SKEY = jwtProvider.getSecretKey();
        this.ISSUER = jwtProvider.getIssuer();
        log.info("[TokenProvider] initialized. issuer={}", ISSUER);
    }

    // =========================
    // 토큰 발급
    // =========================

    /** Access Token 생성 */
    public String createAccessToken(Long subjectId,
                                    TokenSubjectType subjectType,
                                    String comId,
                                    List<String> roles) {

        long now = System.currentTimeMillis();
        Date exp = new Date(now + ACCESS_TOKEN_EXPIRE_TIME_MS);

        Claims claims = Jwts.claims()
                // sub 에는 "subjectId"를 문자열로 넣는 걸 추천
                .setSubject(String.valueOf(subjectId));

        claims.put(CLAIM_SUBJECT_TYPE, subjectType.name());
        if (StringUtils.hasText(comId)) claims.put(CLAIM_COM_ID, comId);

        // roles -> "ROLE_A,ROLE_B"
        if (roles != null && !roles.isEmpty()) {
            claims.put(CLAIM_AUTH, String.join(",", roles));
        }

        claims.put(CLAIM_TOKEN_KIND, "A");

        return Jwts.builder()
                .setIssuer(ISSUER)
                .setIssuedAt(new Date(now))
                .setClaims(claims)
                .setExpiration(exp)
                .signWith(SKEY, SignatureAlgorithm.HS512)
                .compact();
    }

    /** Refresh Token 생성 (권한(auth)은 보통 안 넣음) */
    public String createRefreshToken(Long subjectId,
                                     TokenSubjectType subjectType,
                                     String comId) {

        long now = System.currentTimeMillis();
        Date exp = new Date(now + REFRESH_TOKEN_EXPIRE_TIME_MS);

        Claims claims = Jwts.claims()
                .setSubject(String.valueOf(subjectId));

        claims.put(CLAIM_SUBJECT_TYPE, subjectType.name());
        if (StringUtils.hasText(comId)) claims.put(CLAIM_COM_ID, comId);

        claims.put(CLAIM_TOKEN_KIND, "R");

        return Jwts.builder()
                .setIssuer(ISSUER)
                .setIssuedAt(new Date(now))
                .setClaims(claims)
                .setExpiration(exp)
                .signWith(SKEY, SignatureAlgorithm.HS512)
                .compact();
    }

    // =========================
    // 만료시간(초) getter (TokenService에서 씀)
    // =========================

    public long getAccessExpSeconds() {
        return ACCESS_TOKEN_EXPIRE_TIME_MS / 1000L;
    }

    public long getRefreshExpSeconds() {
        return REFRESH_TOKEN_EXPIRE_TIME_MS / 1000L;
    }

    /** (예전 코드 호환) Refresh 만료 LocalDateTime */
    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plus(REFRESH_TOKEN_EXPIRE_TIME_MS, ChronoUnit.MILLIS);
    }

    // =========================
    // 검증/파싱
    // =========================

    /** 유효성 검증(유효하면 true, 아니면 TokenException 던짐) */
    public boolean validateToken(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                throw new TokenException("토큰이 비어있습니다.");
            }

            Jwts.parserBuilder()
                    .setSigningKey(SKEY)
                    .build()
                    .parseClaimsJws(token);

            return true;

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("[TokenProvider] 잘못된 JWT 서명/형식", e);
            throw new TokenException("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.warn("[TokenProvider] 만료된 JWT 토큰 exp={}", e.getClaims().getExpiration());
            throw new TokenException("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("[TokenProvider] 지원되지 않는 JWT", e);
            throw new TokenException("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("[TokenProvider] JWT 토큰이 잘못되었습니다.", e);
            throw new TokenException("JWT 토큰이 잘못되었습니다.");
        }
    }

    /** Claims 파싱 (만료여도 Claims는 반환) */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SKEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    // =========================
    // Authentication 생성
    // =========================

    /**
     * AccessToken에서 Authentication 생성
     * - roles(auth)가 없으면 인증 객체 생성 불가로 처리(정책에 따라 바꿔도 됨)
     */
    public Authentication getAuthentication(String token) {

        Claims claims = parseClaims(token);

        String subjectIdStr = claims.getSubject();
        String subjectTypeStr = (String) claims.get(CLAIM_SUBJECT_TYPE);
        String comId = (String) claims.get(CLAIM_COM_ID);
        String authStr = (String) claims.get(CLAIM_AUTH);

        if (!StringUtils.hasText(subjectIdStr) || !StringUtils.hasText(subjectTypeStr)) {
            throw new TokenException("토큰 필수 클레임(subjectId/subjectType)이 없습니다.");
        }
        if (!StringUtils.hasText(authStr)) {
            throw new TokenException("권한 정보가 없는 토큰입니다.");
        }

        Long subjectId = Long.valueOf(subjectIdStr);
        TokenSubjectType subjectType = TokenSubjectType.valueOf(subjectTypeStr);

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(authStr.split(","))
                        .filter(StringUtils::hasText)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // ✅ CustomUser에 subjectId/subjectType/comId/authorities 세팅
        // CustomUser에 builder가 있으면 이대로 쓰면 됨.
        CustomUser principal = CustomUser.builder()
                .subjectId(subjectId)
                .subjectType(subjectType)
                .comId(comId)
                .authorities(authorities)
                .build();

        // 만약 CustomUser에 builder/세터가 다르면 아래처럼 맞춰:
        // CustomUser principal = new CustomUser();
        // principal.setSubjectId(subjectId);
        // principal.setSubjectType(subjectType);
        // principal.setComId(comId);
        // principal.setAuthorities(authorities);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    // =========================
    // 토큰에서 값 꺼내기 (필요시)
    // =========================

    public Long getSubjectId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public TokenSubjectType getSubjectType(String token) {
        String v = (String) parseClaims(token).get(CLAIM_SUBJECT_TYPE);
        return TokenSubjectType.valueOf(v);
    }

    public String getComId(String token) {
        return (String) parseClaims(token).get(CLAIM_COM_ID);
    }

    public List<String> getRoles(String token) {
        String auth = (String) parseClaims(token).get(CLAIM_AUTH);
        if (!StringUtils.hasText(auth)) return List.of();
        return Arrays.stream(auth.split(",")).filter(StringUtils::hasText).toList();
    }

    public String getTokenKind(String token) {
        Object kind = parseClaims(token).get(CLAIM_TOKEN_KIND);
        return kind == null ? null : String.valueOf(kind);
    }
}