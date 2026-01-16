package com.multi.mlpenterpriseapprovalsystem.common.jwt.service;

import com.multi.mlpenterpriseapprovalsystem.auth.domain.RefreshToken;
import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.auth.repository.RefreshTokenRepository;
import com.multi.mlpenterpriseapprovalsystem.chat.redis.RedisUtil;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.exception.TokenException;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.enums.MsgStat;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
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
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

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

    private final CompanyRepository companyRepository;
    // 운영 https면 true, 로컬 http면 false
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    private final EmployeeRepository employeeRepository;

    private final RedisUtil redisUtil;

    private final org.springframework.beans.factory.ObjectProvider<TokenService> selfProvider;

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
//    @Transactional
//    public void logout(HttpServletRequest request, HttpServletResponse response) {
//
//        try {
//            // 1) refreshToken 쿠키 추출
//            String refreshToken = extractCookie(request, "refreshToken").orElse(null);
//            String accessToken = extractCookie(request, "accessToken").orElse(null);
//
//            // ✅ 형식이 JWT가 아니면(= '.' 2개가 아니면) revoke/파싱 자체를 하지 않고 종료
//            if (refreshToken == null || refreshToken.isBlank()
//                    || refreshToken.chars().filter(ch -> ch == '.').count() != 2) {
//                log.warn("[LOGOUT] skip revoke. invalid token format token={}", refreshToken);
//                return; // 여기서 끝(=로그아웃 성공 취급)
//            }
//
//            // 2) RT 서명/만료 검증 (✅ validateToken 자체가 throw 할 수도 있으니 catch)
//            boolean valid;
//            try {
//                valid = tokenProvider.validateToken(refreshToken);
//            } catch (Exception e) {
//                log.warn("[LOGOUT] validateToken threw exception. skip revoke. token={}", refreshToken, e);
//                return; // 쿠키는 finally에서 삭제됨
//            }
//
//            if (!valid) {
//                log.info("[LOGOUT] refreshToken not valid. skip revoke.");
//                return;
//            }
//
//            // 3) Claims 파싱 (✅ parseClaims도 throw 가능)
//            Claims rtClaims;
//            try {
//                rtClaims = tokenProvider.parseClaims(refreshToken);
//            } catch (Exception e) {
//                log.warn("[LOGOUT] parseClaims failed. skip revoke. token={}", refreshToken, e);
//                return;
//            }
//
//            // 4) subject / type 파싱도 안전하게
//            Long subjectId;
//            TokenSubjectType subjectType;
//            try {
//                subjectId = tokenProvider.getSubjectId(rtClaims.getSubject());
//                subjectType = tokenProvider.getSubjectType(rtClaims.getSubject());
//            } catch (Exception e) {
//                log.warn("[LOGOUT] subject parsing failed. skip revoke. subject={}", rtClaims.getSubject(), e);
//                return;
//            }
//
//            // ✅ 직원 로그아웃이면 msgStat OFF 처리 (이것도 실패해도 로그아웃 실패로 만들지 않음)
//            try {
//                if (subjectType == TokenSubjectType.EMPLOYEE) {
//                    employeeRepository.updateMsgStatByEmpNo(subjectId, MsgStat.OFF);
//                }
//            } catch (Exception e) {
//                log.warn("[LOGOUT] updateMsgStat failed. continue logout. subjectId={}", subjectId, e);
//            }
//
//            // 5) DB에 저장된 RT revoke (이것도 실패해도 쿠키삭제는 진행)
//            try {
//                var stored = refreshTokenRepository
//                        .findAllBySubjectTypeAndSubjectIdAndRevokedFalse(subjectType, subjectId);
//                stored.forEach(RefreshToken::revoke);
//            } catch (Exception e) {
//                log.warn("[LOGOUT] revoke stored refresh tokens failed. continue logout. subjectId={}", subjectId, e);
//            }
//
//            // Access 블랙리스트
//            if (StringUtils.hasText(accessToken)) {
//                String jti = tokenProvider.getJti(accessToken);
//                long ttl = tokenProvider.getRemainingSecondsForBlacklist(accessToken);
//
//                if (StringUtils.hasText(jti) && ttl > 0) {
//                    String key = tokenProvider.blacklistKeyForAccessJti(jti); // bl:at:{jti}
//                    redisUtil.setBlackListSeconds(key, "logout", ttl);
//                }
//            }
//
//            // Refresh 블랙리스트
//            if (StringUtils.hasText(refreshToken)) {
//                String jti = tokenProvider.getJti(refreshToken);
//                long ttl = tokenProvider.getRemainingSecondsForBlacklist(refreshToken);
//
//                if (StringUtils.hasText(jti) && ttl > 0) {
//                    String key = tokenProvider.blacklistKeyForRefreshJti(jti); // bl:rt:{jti}
//                    redisUtil.setBlackListSeconds(key, "logout", ttl);
//                }
//            }
//        } catch (Exception e) {
//            // ✅ logout은 어떤 예외가 와도 500으로 터지면 UX 최악이라 "먹고" 쿠키만 지우게 한다
//            log.warn("[LOGOUT] unexpected error. force clear cookies. ", e);
//
//        } finally {
//            // ✅ 성공/실패 상관없이 쿠키는 무조건 삭제
//            clearAuthCookies(response);
//        }
//    }
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = null;
        String accessToken = null;

        try {
            // 0) 쿠키 추출
            refreshToken = extractCookie(request, "refreshToken").orElse(null);
            accessToken = extractCookie(request, "accessToken").orElse(null);

            // 0-1) 유입 확인 로그 (쿠키/헤더/길이)
            log.info("[LOGOUT] ===== START =====");
            log.info("[LOGOUT] uri={}, method={}", request.getRequestURI(), request.getMethod());
            log.info("[LOGOUT] cookieHeader={}", request.getHeader("Cookie"));
            log.info("[LOGOUT] authHeader={}", request.getHeader("Authorization"));
            log.info("[LOGOUT] extracted accessToken?={}, refreshToken?={}",
                    org.springframework.util.StringUtils.hasText(accessToken),
                    org.springframework.util.StringUtils.hasText(refreshToken));
            log.info("[LOGOUT] accessLen={}, refreshLen={}",
                    accessToken == null ? -1 : accessToken.length(),
                    refreshToken == null ? -1 : refreshToken.length());

            // =========================================================
            // ✅ (A) AccessToken 블랙리스트는 RT 상태와 무관하게 "독립적으로" 처리
            //     (숨은 함정 해결: RT가 없/이상해도 AT 블랙리스트는 넣는다)
            // =========================================================
            if (org.springframework.util.StringUtils.hasText(accessToken)) {
                try {
                    // JWT 형식 간단 체크 (형식 이상이면 파싱 자체 스킵)
                    long dotCount = accessToken.chars().filter(ch -> ch == '.').count();
                    if (dotCount != 2) {
                        log.warn("[LOGOUT] AT invalid format (dotCount={}). skip AT blacklist.", dotCount);
                    } else {
                        String atJti = tokenProvider.getJti(accessToken);
                        long atTtl = tokenProvider.getRemainingSecondsForBlacklist(accessToken); // 남은 초
                        String atKey = tokenProvider.blacklistKeyForAccessJti(atJti);           // bl:at:{jti}

                        log.info("[LOGOUT] AT parsed. jti={}, ttlSec={}, key={}", atJti, atTtl, atKey);

                        if (org.springframework.util.StringUtils.hasText(atJti) && atTtl > 0) {
                            redisUtil.setBlackListSeconds(atKey, "logout", atTtl);
                            boolean exists = redisUtil.hasKeyBlackList(atKey); // 있으면 true
                            log.info("[LOGOUT] AT blacklist set OK. exists={}", exists);
                        } else {
                            log.warn("[LOGOUT] AT blacklist skip. jtiBlank?={}, ttlSec={}",
                                    !org.springframework.util.StringUtils.hasText(atJti), atTtl);
                        }
                    }
                } catch (Exception e) {
                    log.warn("[LOGOUT] AT blacklist failed. continue logout.", e);
                }
            } else {
                log.info("[LOGOUT] AT cookie empty. skip AT blacklist.");
            }

            // =========================================================
            // ✅ (B) RefreshToken 관련 처리는 "있을 때만" 최대한 시도 (DB revoke + RT 블랙리스트)
            //     RT가 없/이상해도 로그아웃 UX는 성공 취급, 쿠키는 finally에서 삭제
            // =========================================================
            if (!org.springframework.util.StringUtils.hasText(refreshToken)) {
                log.warn("[LOGOUT] RT cookie empty. skip RT revoke/blacklist.");
                return;
            }

            // JWT 형식 체크 (형식 이상이면 RT 관련 로직은 스킵)
            long rtDotCount = refreshToken.chars().filter(ch -> ch == '.').count();
            if (rtDotCount != 2) {
                log.warn("[LOGOUT] RT invalid format (dotCount={}). skip RT revoke/blacklist.");
                return;
            }

            // 1) RT 검증
            boolean rtValid;
            try {
                rtValid = tokenProvider.validateToken(refreshToken);
                log.info("[LOGOUT] RT validateToken={}", rtValid);
            } catch (Exception e) {
                log.warn("[LOGOUT] RT validateToken threw. skip RT revoke/blacklist.", e);
                return;
            }

            if (!rtValid) {
                log.info("[LOGOUT] RT not valid. skip RT revoke/blacklist.");
                return;
            }

            // 2) RT Claims 파싱
            Claims rtClaims;
            try {
                rtClaims = tokenProvider.parseClaims(refreshToken);
            } catch (Exception e) {
                log.warn("[LOGOUT] RT parseClaims failed. skip RT revoke/blacklist.", e);
                return;
            }

            // 3) subject 파싱
            Long subjectId;
            TokenSubjectType subjectType;
            try {
                subjectId = tokenProvider.getSubjectIdFromClaims(rtClaims);
                subjectType = tokenProvider.getSubjectTypeFromClaims(rtClaims);
                log.info("[LOGOUT] RT subject parsed. subjectType={}, subjectId={}", subjectType, subjectId);
            } catch (Exception e) {
                log.warn("[LOGOUT] RT subject parsing failed. skip RT revoke/blacklist.", e);
                return;
            }

            // 4) (직원) 메시지 상태 OFF (실패해도 계속)
            try {
                if (subjectType == TokenSubjectType.EMPLOYEE) {
                    employeeRepository.updateMsgStatByEmpNo(subjectId, MsgStat.OFF);
                    log.info("[LOGOUT] EMP msgStat OFF updated. empNo={}", subjectId);
                }
            } catch (Exception e) {
                log.warn("[LOGOUT] updateMsgStat failed. continue logout. subjectId={}", subjectId, e);
            }

            // 5) DB RT revoke (실패해도 계속)
            try {
                var stored = refreshTokenRepository
                        .findAllBySubjectTypeAndSubjectIdAndRevokedFalse(subjectType, subjectId);
                int before = stored == null ? 0 : stored.size();
                if (stored != null) stored.forEach(RefreshToken::revoke);
                log.info("[LOGOUT] DB RT revoked. count={}", before);
            } catch (Exception e) {
                log.warn("[LOGOUT] revoke stored refresh tokens failed. continue logout. subjectId={}", subjectId, e);
            }

            // 6) RT 블랙리스트 (실패해도 계속)
            try {
                String rtJti = tokenProvider.getJti(refreshToken);
                long rtTtl = tokenProvider.getRemainingSecondsForBlacklist(refreshToken);
                String rtKey = tokenProvider.blacklistKeyForRefreshJti(rtJti); // bl:rt:{jti}

                log.info("[LOGOUT] RT parsed. jti={}, ttlSec={}, key={}", rtJti, rtTtl, rtKey);

                if (org.springframework.util.StringUtils.hasText(rtJti) && rtTtl > 0) {
                    redisUtil.setBlackListSeconds(rtKey, "logout", rtTtl);
                    boolean exists = redisUtil.hasKeyBlackList(rtKey);
                    log.info("[LOGOUT] RT blacklist set OK. exists={}", exists);
                } else {
                    log.warn("[LOGOUT] RT blacklist skip. jtiBlank?={}, ttlSec={}",
                            !org.springframework.util.StringUtils.hasText(rtJti), rtTtl);
                }
            } catch (Exception e) {
                log.warn("[LOGOUT] RT blacklist failed. continue logout.", e);
            }

        } catch (Exception e) {
            // ✅ logout은 어떤 예외가 와도 500으로 터지면 UX 최악이라 "먹고" 쿠키만 지우게 한다
            log.warn("[LOGOUT] unexpected error. force clear cookies.", e);

        } finally {
            // ✅ 성공/실패 상관없이 쿠키는 무조건 삭제
            try {
                clearAuthCookies(response);
                log.info("[LOGOUT] cookies cleared.");
            } catch (Exception e) {
                log.warn("[LOGOUT] clearAuthCookies failed.", e);
            }
            log.info("[LOGOUT] ===== END =====");
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

        log.info("[Refresh] cookieHeader={}", request.getHeader("Cookie")); // ✅ 중복 쿠키 확인용

        String refreshToken = extractCookie(request, "refreshToken")
                .orElseThrow(() -> {
                    log.warn("[Refresh] step0: refreshToken cookie missing");
                    return new CustomException(ErrorCode.UNAUTHORIZED);
                });

        // ✅ 쿠키 목록(이름/길이)
        if (request.getCookies() == null) {
            log.info("[Refresh] cookies = null");
        } else {
            log.info("[Refresh] cookies = {}", Arrays.stream(request.getCookies())
                    .map(c -> c.getName() + "=" + (c.getValue() == null ? "null" : "len=" + c.getValue().length()))
                    .toList());
        }

        // ✅ RT 기본 정보(길이/해시 앞 8자리)
        log.info("[Refresh] step1: rtLen={}, rtHash={}",
                refreshToken.length(),
                DigestUtils.md5DigestAsHex(refreshToken.getBytes()).substring(0, 8));

        // 0) RT 검증 (서명/만료)
        try {
            boolean ok = tokenProvider.validateToken(refreshToken);
            log.info("[Refresh] step2: validateToken ok={}", ok);

            if (!ok) {
                log.warn("[Refresh] step2-1: validateToken returned false");
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
        } catch (TokenException e) {
            log.error("[Refresh] step2-2: validateToken FAILED msg={}", e.getMessage(), e);
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 1) Refresh 토큰인지 확인
        String kind = tokenProvider.getTokenKind(refreshToken);
        log.info("[Refresh] step3: tokenKind={}", kind);

        if (!"R".equals(kind)) {
            log.warn("[Refresh] step3-1: tokenKind is not R");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 2) ✅ RT 블랙리스트(jti) 체크
        String rtJti = tokenProvider.getJti(refreshToken);
        log.info("[Refresh] step4: rtJti={}", rtJti);

        if (!StringUtils.hasText(rtJti)) {
            log.warn("[Refresh] step4-1: rtJti is empty");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        String rtBlKey = tokenProvider.blacklistKeyForRefreshJti(rtJti); // bl:rt:{jti}
        boolean black = redisUtil.hasKeyBlackList(rtBlKey);
        log.info("[Refresh] step5: rtBlKey={}, exists={}", rtBlKey, black);

        if (black) {
            log.warn("[Refresh] step5-1: refresh token is blacklisted");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 3) Claims에서 subject 정보 추출
        Claims rtClaims = tokenProvider.parseClaims(refreshToken);

        Long subjectId = tokenProvider.getSubjectIdFromClaims(rtClaims);
        TokenSubjectType subjectType = tokenProvider.getSubjectTypeFromClaims(rtClaims);
        log.info("[Refresh] step6: subjectType={}, subjectId={}", subjectType, subjectId);

        String comId;
        String username;
        List<String> roles;

        if (subjectType == TokenSubjectType.EMPLOYEE) {
            Employee emp = employeeRepository.findById(subjectId)
                    .orElseThrow(() -> {
                        log.warn("[Refresh] step7: employee not found subjectId={}", subjectId);
                        return new CustomException(ErrorCode.UNAUTHORIZED);
                    });

            comId = emp.getCompany().getComId();
            username = emp.getEmpId();

            log.info("[Refresh] step7-emp: comId={}, username={}, role={}", comId, username, emp.getRole());

            if (emp.getRole() == null) {
                log.warn("[Refresh] step7-1: employee role is null subjectId={}", subjectId);
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
            roles = List.of("ROLE_" + emp.getRole().name());

        } else if (subjectType == TokenSubjectType.COMPANY) {
            Company com = companyRepository.findById(subjectId)
                    .orElseThrow(() -> {
                        log.warn("[Refresh] step7: company not found subjectId={}", subjectId);
                        return new CustomException(ErrorCode.UNAUTHORIZED);
                    });

            comId = com.getComId();
            username = com.getEmail();

            log.info("[Refresh] step7-com: comId={}, username={}, role={}", comId, username, com.getRole());

            if (com.getRole() == null) {
                log.warn("[Refresh] step7-1: company role is null subjectId={}", subjectId);
                throw new CustomException(ErrorCode.UNAUTHORIZED);
            }
            roles = List.of("ROLE_" + com.getRole().name());
        } else {
            log.warn("[Refresh] step7-2: invalid subjectType={}", subjectType);
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // [Step 6, 7까지는 동일하게 진행하여 comId, username, roles 정보를 미리 준비해둡니다]

        String lockKey = (subjectType.name() + ":" + subjectId).intern();
        synchronized (lockKey) {
            return selfProvider.getObject().proceedRefresh(refreshToken, rtBlKey, subjectId, subjectType, comId, username, roles, response);
        }
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

    @Transactional
    public ResTokenDto proceedRefresh(String refreshToken, String rtBlKey, Long subjectId, TokenSubjectType subjectType, String comId, String username, List<String> roles, HttpServletResponse response) {
        // 4) DB에서 현재 유효 RT(최신 revoked=false) 조회
        RefreshToken dbRT = refreshTokenRepository
                .findTopBySubjectTypeAndSubjectIdAndRevokedFalseOrderByRefNoDesc(subjectType, subjectId)
                .orElseThrow(() -> {
                    log.warn("[Refresh] step8: dbRT not found subjectType={}, subjectId={}", subjectType, subjectId);
                    return new CustomException(ErrorCode.UNAUTHORIZED);
                });

        log.info("[Refresh] step9: dbRT refNo={}, revoked={}, expiredAt={}, dbHash={}",
                dbRT.getRefNo(),
                dbRT.isRevoked(),
                dbRT.getExpiredAt(),
                DigestUtils.md5DigestAsHex(dbRT.getToken().getBytes()).substring(0, 8));

        // 4-1) DB 만료/폐기 체크
        if (dbRT.isRevoked() || LocalDateTime.now().isAfter(dbRT.getExpiredAt())) {
            log.warn("[Refresh] step9-1: dbRT revoked/expired. revoked={}, now={}, expiredAt={}",
                    dbRT.isRevoked(), LocalDateTime.now(), dbRT.getExpiredAt());
            dbRT.revoke();
            refreshTokenRepository.save(dbRT);
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        // 4-2) DB의 RT와 클라이언트 RT 일치 체크
        boolean equals = dbRT.getToken().equals(refreshToken);
        log.info("[Refresh] step10: tokenEquals={}", equals);

        // =====================================================================
        // 🚀 [여기서부터 사용자님의 요청대로 '이해하기 쉽게' 추가된 핵심 로직]
        // =====================================================================
        if (!equals) {
            // 💡 [해설] 락(Lock) 때문에 줄 서있다가 들어왔는데, DB 토큰이 내가 가져온 토큰과 다르다?
            // -> "이미 내 앞의 요청(Thread)이 토큰을 새걸로 바꿔치기 완료했다!"는 뜻입니다.
            log.info("[Refresh] step10-A: 이미 다른 요청에 의해 갱신된 토큰 발견. 최신 토큰을 재사용합니다.");

            // 1등이 만들어둔 따끈따끈한 새 토큰(dbRT)을 내 쿠키에도 똑같이 구워줍니다.
            setRefreshCookie(response, dbRT.getToken());
            String currentAt = tokenProvider.createAccessToken(subjectId, subjectType, comId, username, roles);
            setAccessCookie(response, currentAt);

            log.info("[Refresh] step10-B: 재사용 응답 완료 SUCCESS subjectId={}", subjectId);

            return ResTokenDto.builder()
                    .accessToken(currentAt)
                    .expiresInSeconds(tokenProvider.getAccessExpSeconds())
                    .subjectType(subjectType.name())
                    .role(roles.isEmpty() ? null : roles.get(0))
                    .build();
            // ⚠️ 여기서 바로 리턴하여 아래의 "기존 토큰 폐기 및 새로 만들기" 로직을 건너뜁니다.
        }
        // =====================================================================

        // ==============================
        // ✅ 여기부터가 "진짜 1등"만 수행하는 로테이션 핵심 (기존 로그/로직 100% 유지)
        // ==============================

        // 5) 기존 RT를 즉시 폐기 (재사용 방지)
        log.info("[Refresh] step11: revoke old dbRT refNo={}", dbRT.getRefNo());
        dbRT.revoke();
        refreshTokenRepository.saveAndFlush(dbRT); // 💡 Flush를 해서 뒷사람들이 바로 알게 함

        // 5-1) 기존 RT도 Redis 블랙리스트에 넣기
        long oldRtTtl = tokenProvider.getRemainingSecondsForBlacklist(refreshToken);
        log.info("[Refresh] step12: put old RT to blacklist key={}, ttl={}", rtBlKey, oldRtTtl);
        if (oldRtTtl > 0) {
            redisUtil.setBlackListSeconds(rtBlKey, "rotated", oldRtTtl);
        }

        // 6) 새 RT 발급 + DB 저장 + 쿠키 세팅
        String newRefreshToken = tokenProvider.createRefreshToken(subjectId, subjectType, comId);
        log.info("[Refresh] step13: newRtLen={}, newRtHash={}",
                newRefreshToken.length(),
                DigestUtils.md5DigestAsHex(newRefreshToken.getBytes()).substring(0, 8));

        setRefreshCookie(response, newRefreshToken);

        LocalDateTime newRtExpiredAt = LocalDateTime.now().plusSeconds(tokenProvider.getRefreshExpSeconds());
        log.info("[Refresh] step14: newRtExpiredAt={}", newRtExpiredAt);

        RefreshToken newDbRt = RefreshToken.builder()
                .subjectType(subjectType)
                .subjectId(subjectId)
                .token(newRefreshToken)
                .expiredAt(newRtExpiredAt)
                .build();

        RefreshToken savedNewRt = refreshTokenRepository.save(newDbRt);
        log.info("[Refresh] step15: saved new dbRT refNo={}", savedNewRt.getRefNo());

        // 7) 새 AT 발급 + 쿠키 세팅
        String newAccessToken = tokenProvider.createAccessToken(subjectId, subjectType, comId, username, roles);
        log.info("[Refresh] step16: newAtLen={}, newAtHash={}",
                newAccessToken.length(),
                DigestUtils.md5DigestAsHex(newAccessToken.getBytes()).substring(0, 8));

        setAccessCookie(response, newAccessToken);

        log.info("[Refresh] step17: SUCCESS subjectType={}, subjectId={}", subjectType, subjectId);

        return ResTokenDto.builder()
                .accessToken(newAccessToken)
                .expiresInSeconds(tokenProvider.getAccessExpSeconds())
                .subjectType(subjectType.name())
                .role(roles.isEmpty() ? null : roles.get(0))
                .build();
    }

}