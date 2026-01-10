package com.multi.mlpenterpriseapprovalsystem.common.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.RedisUtil;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ApiExceptionDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.TokenException;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.service.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * JwtFilter
 *
 * @author : 권지영
 * @filename : JwtFilter
 * @since : 2025. 12. 17. 수요일
 */
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;
    private final TokenService tokenService;
    private final RedisUtil redisUtil;

    public JwtFilter(TokenProvider tokenProvider, TokenService tokenService, RedisUtil redisUtil) {

        this.tokenProvider = tokenProvider;
        this.tokenService = tokenService;
        this.redisUtil = redisUtil;
    }

    private static final String[] EXACT_PATHS = {
            "/health-check"
    };

    private static final String[] WILDCARD_PATHS = {
            "/auth/companies/**",
            "/auth/employee/**",
            "/auth/refresh",
            "/refresh",
            "/auth/logout",
            "/auth/password",
            "/auth/verify/**",
            "/public/**",
            "/swagger-ui/**"
    };


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("[JwtFilter] doFilterInternal START ===================================");
        String requestURI = request.getRequestURI();

        try {
            // 정확히 일치하는 경로는 필터를 건너뜀
            for (String exactPath : EXACT_PATHS) {
                if (requestURI.equals(exactPath)) {
                    filterChain.doFilter(request, response);
                    log.info("[JwtFilter] 요청 URI가 제외 경로에 해당하여 필터를 건너뜁니다.");

                    return;
                }
            }

            // 와일드카드 경로 매칭
            for (String wildcardPath : WILDCARD_PATHS) {
                if (requestURI.matches(wildcardPath.replace("**", ".*"))) {
                    log.info("[JwtFilter] 요청 URI가 제외 경로에 해당하여 필터를 건너뜁니다.");

                    filterChain.doFilter(request, response);
                    return;
                }
            }

            String jwt = resolveToken(request);
            log.info("[JwtFilter] jwt : {}", jwt);

            // ✅ 0) Access 토큰이 있으면 먼저 블랙리스트 확인 (로그아웃 토큰 차단)
            if (StringUtils.hasText(jwt)) {
                if (isAccessBlacklisted(jwt)) {
                    log.warn("[JwtFilter] AccessToken is blacklisted. deny request. uri={}", requestURI);

                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    ApiExceptionDto errorResponse = new ApiExceptionDto(HttpStatus.UNAUTHORIZED, "로그아웃된 토큰입니다.");
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.getWriter().write(convertObjectToJson(errorResponse));
                    response.getWriter().flush();
                    return; // ✅ 여기서 차단 (refresh 시도도 하지 않음)
                }
            }
            // 1. Access Token이 있고 유효한 경우 -> 그대로 진행
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

                log.info("[JwtFilter] JWT 토큰이 존재합니다.");
                authenticateUser(jwt, request);
            }
            // 2. Access Token이 없거나 만료된 경우 -> Refresh 시도
            else {
                log.info("[JwtFilter] Access Token 만료/부재. 자동 재발급 시도...");
                try {
                    // 내부에서 RT 검증, DB 체크, 새 AT 쿠키 설정까지 다 수행함
                    ResTokenDto resTokenDto = tokenService.refreshAccessToken(request, response);

                    String newAt = resTokenDto.getAccessToken();
                    if (newAt != null) {
                        authenticateUser(newAt, request);
                        log.info("[JwtFilter] 자동 재발급 및 인증 성공");
                    }
                } catch (Exception e) {
                    // 리프레시 토큰도 없거나 만료된 경우 (진짜 비로그인 상태)
                    log.info("[JwtFilter] 자동 재발급 실패: {}", e.getMessage());
                    // 여기서 아무 처리를 안 하면 '익명 사용자'로 filterChain을 타게 됨
                }
            }

            // 4. 필터 체인 계속 진행
            filterChain.doFilter(request, response);
            log.info("[JwtFilter] 필터 체인 완료 후 응답 처리");

        } catch (TokenException e) {
            log.error("[JwtFilter] 필터 처리 중 예외 발생: {}", e.getMessage(), e);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            ApiExceptionDto errorResponse = new ApiExceptionDto(HttpStatus.UNAUTHORIZED, e.getMessage());
            // 예외(에러)가 발생했을 때 클라이언트에게 보내주는 표준 응답 객체

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write(convertObjectToJson(errorResponse));
            response.getWriter().flush();
        }
    }

    // 중복 로직을 별도 메서드로 분리
    private void authenticateUser(String jwt, HttpServletRequest request) {
        Claims claims = tokenProvider.parseClaims(jwt);
        String comId = (String) claims.get("comId");
        request.setAttribute("comId", comId);
        log.info("[JwtFilter] request에 comId 세팅 완료: {}", comId);

        Authentication authentication = tokenProvider.getAuthentication(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("[JwtFilter] SecurityContext에 Authentication 객체 설정 완료: {}", authentication.getName());
        log.info("[JwtFilter] SecurityContext에 Authentication 객체 설정 완료  authentication.getAuthorities(): {}", authentication.getAuthorities());

        log.info("[JwtFilter] SecurityContextHolder 객체 확인: {}", SecurityContextHolder.getContext().getAuthentication());
    }

    // 쿠키 설정 편의 메서드
    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie("accessToken", accessToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60); // 1분 (테스트용)
        response.addCookie(cookie);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization 헤더 우선
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // 2) 없으면 accessToken 쿠키에서 찾기
        return extractCookie(request, "accessToken").orElse(null);
    }

    public String convertObjectToJson(Object object) throws JsonProcessingException {
        if (object == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();

        for (Cookie c : cookies) {
            // 모든 쿠키를 다 찍어서 서버가 뭘 보고 있는지 확인하세요
            log.info("[JwtFilter] 발견된 쿠키 - 이름: {}, 값: {}", c.getName(), c.getValue());
        }

        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private boolean isAccessBlacklisted(String accessToken) {
        String jti = tokenProvider.getJti(accessToken);
        if (!StringUtils.hasText(jti)) return false; // jti 없으면 블랙리스트 체크 불가 -> 일단 false
        String key = tokenProvider.blacklistKeyForAccessJti(jti); // bl:at:{jti}
        return redisUtil.hasKeyBlackList(key);
    }

}

