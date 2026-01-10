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
import java.util.*;
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
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_JTI = "jti";

    // ===== Expire =====
    private static final long ACCESS_TOKEN_EXPIRE_TIME_MS = 1000L * 60 * 60 * 24;  // 1일
    private static final long REFRESH_TOKEN_EXPIRE_TIME_MS = 1000L * 60 * 60 * 24 * 7; // 7일 (원하면 늘려)
    // 테스트용: Access 1분, Refresh 3분
//    private static final long ACCESS_TOKEN_EXPIRE_TIME_MS  = 1000L * 60 * 1;  // 1분
//    private static final long REFRESH_TOKEN_EXPIRE_TIME_MS = 1000L * 60 * 3;  // 3분

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

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long subjectId,
                                    TokenSubjectType subjectType,
                                    String comId,
                                    String username,
                                    List<String> roles) {

        long now = System.currentTimeMillis();
        Date exp = new Date(now + ACCESS_TOKEN_EXPIRE_TIME_MS);

        Claims claims = Jwts.claims()
                // sub 에는 "subjectId"를 문자열로 넣는 걸 추천
                .setSubject(String.valueOf(subjectId));

        claims.put(CLAIM_SUBJECT_TYPE, subjectType.name());
        if (StringUtils.hasText(comId)) claims.put(CLAIM_COM_ID, comId);

        claims.put(CLAIM_USERNAME, username);

        // roles -> "ROLE_A,ROLE_B"
        if (roles != null && !roles.isEmpty()) {
            claims.put(CLAIM_AUTH, String.join(",", roles));
        }

        claims.put(CLAIM_TOKEN_KIND, "A");
        claims.put(CLAIM_JTI, UUID.randomUUID().toString());

        return Jwts.builder()
                .setIssuer(ISSUER)
                .setIssuedAt(new Date(now))
                .setClaims(claims)
                .setExpiration(exp)
                .signWith(SKEY, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Refresh Token 생성 (권한(auth)은 보통 안 넣음)
     */
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
        claims.put(CLAIM_JTI, UUID.randomUUID().toString());

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

    /**
     * (예전 코드 호환) Refresh 만료 LocalDateTime
     */
    public LocalDateTime getRefreshTokenExpiry() {
        return LocalDateTime.now().plus(REFRESH_TOKEN_EXPIRE_TIME_MS, ChronoUnit.MILLIS);
    }

    // =========================
    // 검증/파싱
    // =========================

    public boolean validateTokenSignatureAndExp(String token) {
        if (!StringUtils.hasText(token) || token.chars().filter(ch -> ch == '.').count() != 2) {
            return false;
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SKEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    /**
     * 유효성 검증(유효하면 true, 아니면 TokenException 던짐)
     */
    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token) || token.chars().filter(ch -> ch == '.').count() != 2) {
            log.error("유효하지 않은 JWT 형식입니다. (점 개수 부족)");
            return false;
        }
        try {

            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(SKEY)
                    .build()
                    .parseClaimsJws(token);

            // 2) ✅ 블랙리스트(jti) 체크
            Claims claims = jws.getBody(); // 여기서는 만료 예외 안 남(이미 위에서 검증됨)
            Object jtiObj = claims.get(CLAIM_JTI);
            if (jtiObj == null) return false;

            String jti = String.valueOf(jtiObj);
            String kind = String.valueOf(claims.get(CLAIM_TOKEN_KIND)); // "A" or "R"

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

    /**
     * Claims 파싱 (만료여도 Claims는 반환)
     */
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

        // ✅ 이 로그를 꼭 찍어보세요!
        log.info("[JwtProvider] 토큰에서 읽어온 전체 클레임: {}", claims);
        log.info("[JwtProvider] CLAIM_AUTH 상수값: {}", CLAIM_AUTH);
        log.info("[JwtProvider] 실제 추출된 authStr: {}", claims.get(CLAIM_AUTH));

        String subjectIdStr = claims.getSubject();
        String subjectTypeStr = (String) claims.get(CLAIM_SUBJECT_TYPE);
        String comId = (String) claims.get(CLAIM_COM_ID);
        String authStr = (String) claims.get(CLAIM_AUTH);
        String userName = (String) claims.get(CLAIM_USERNAME);

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
                .username(userName)
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

    public String getUsername(String token) {
        return (String) parseClaims(token).get(CLAIM_USERNAME);
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

    // TokenProvider.java

    /** Claims 객체에서 subject(ID) 추출 */
    public Long getSubjectIdFromClaims(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /** Claims 객체에서 사용자 유형 추출 */
    public TokenSubjectType getSubjectTypeFromClaims(Claims claims) {
        String v = (String) claims.get(CLAIM_SUBJECT_TYPE);
        return TokenSubjectType.valueOf(v);
    }

    /** Claims 객체에서 회사 ID 추출 */
    public String getComIdFromClaims(Claims claims) {
        return (String) claims.get(CLAIM_COM_ID);
    }

    /** Claims 객체에서 사용자 이름 추출 */
    public String getUsernameFromClaims(Claims claims) {
        return (String) claims.get(CLAIM_USERNAME);
    }

    /** Claims 객체에서 권한 목록 추출 */
    public List<String> getRolesFromClaims(Claims claims) {
        String auth = (String) claims.get(CLAIM_AUTH);
        if (!StringUtils.hasText(auth)) return List.of();
        return Arrays.stream(auth.split(","))
                .filter(StringUtils::hasText)
                .toList();
    }

    /** Claims 객체에서 토큰 종류(A/R) 추출 */
    public String getTokenKindFromClaims(Claims claims) {
        Object kind = claims.get(CLAIM_TOKEN_KIND);
        return kind == null ? null : String.valueOf(kind);
    }

    public String getJti(String token) {
        Object jti = parseClaims(token).get(CLAIM_JTI);
        return jti == null ? null : String.valueOf(jti);
    }

    /** 남은 유효시간(초) - 블랙리스트 TTL로 쓰기 (서명 오류면 0) */
    public long getRemainingSecondsForBlacklist(String token) {
        if (!StringUtils.hasText(token)) return 0L;

        try {
            Claims claims = parseClaims(token); // 서명/형식 오류면 아래에서 예외 가능
            Date exp = claims.getExpiration();
            if (exp == null) return 0L;

            long diffMs = exp.getTime() - System.currentTimeMillis();
            if (diffMs <= 0) return 0L;

            return (diffMs + 999) / 1000L; // ceil
        } catch (JwtException | IllegalArgumentException e) {
            return 0L;
        }
    }

    /** 블랙리스트 키 생성 */
    public String blacklistKeyForAccessJti(String jti) {
        return "bl:at:" + jti;
    }

    public String blacklistKeyForRefreshJti(String jti) {
        return "bl:rt:" + jti;
    }
}