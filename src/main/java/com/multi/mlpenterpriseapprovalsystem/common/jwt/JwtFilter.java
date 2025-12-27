package com.multi.mlpenterpriseapprovalsystem.common.jwt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ApiExceptionDto;
import com.multi.mlpenterpriseapprovalsystem.common.exception.TokenException;
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

    public JwtFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    private static final String[] EXACT_PATHS = {
            "/health-check"
    };

    private static final String[] WILDCARD_PATHS = {
            "/auth/**",
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
            if (StringUtils.hasText(jwt)) {
                log.info("[JwtFilter] JWT 토큰이 존재합니다.");

                if (tokenProvider.validateToken(jwt)) {

                    log.info("[JwtFilter] JWT 토큰이 유효합니다.");

                    Claims claims = tokenProvider.parseClaims(jwt);
                    String comId = (String) claims.get("comId");
                    request.setAttribute("comId", comId);

                    log.info("[JwtFilter] request에 comId 세팅 완료: {}", comId);

                    Authentication authentication = tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.info("[JwtFilter] SecurityContext에 Authentication 객체 설정 완료: {}", authentication);
                    log.info("[JwtFilter] SecurityContext에 Authentication 객체 설정 완료  authentication.getAuthorities(): {}", authentication.getAuthorities());

                    log.info("[JwtFilter] SecurityContextHolder 객체 확인: {}", SecurityContextHolder.getContext().getAuthentication());

                } else {
                    log.warn("[JwtFilter] JWT 토큰이 유효하지 않습니다.");
                }
            } else {
                log.info("[JwtFilter] JWT 토큰이 존재하지 않습니다.");
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

        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

}

