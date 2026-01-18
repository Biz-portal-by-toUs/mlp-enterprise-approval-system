package com.multi.mlpenterpriseapprovalsystem.common.config;

import com.multi.mlpenterpriseapprovalsystem.chat.redis.RedisUtil;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.JwtFilter;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig
 *
 * @author : 권지영
 * @filename : SecurityConfig
 * @since : 2025. 12. 17. 수요일
 */
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    private final TokenProvider tokenProvider;
    private final RedisUtil redisUtil;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokenService) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 이렇게 하면 타임리프에서도 세션 사용안하고 jwt 사용하면 됨
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable) // ✅ 이 줄이 추가되어야 합니다.
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/meetings/*/ai").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/prov-documents/*/embedding").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/chatbot/callback").permitAll()

                        .requestMatchers("/api/v1/notifications/stream", "/api/v1/chatbot/stream").authenticated()

                        .requestMatchers("/api/v1/subscriptions").permitAll()
                        .requestMatchers("/auth/**",
                                "/meeting-rooms/**",
                                "/corporate-cars/**",
                                "/my-reservations",
                                "/shared-equipment/**",
                                "/admin/**",
                                "/attachment-test",
                                "/schedule/**",
                                "/org-chart/**",
                                "/uploads/**",
                                "/images/**",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico",
                                "/employees/**",
                                "/meeting/**",
                                "/attendances/**",
                                "/board/**",
                                "/chatbot",
                                "/cloud/**",
                                "/documents/**",
                                "/document-forms",
                                "/form/**",
                                "/mail/**",
                                "/notice/**",
                                "/notifications",
                                "/payment-methods/**",
                                "/payment-historys",
                                "/subscriptions").permitAll()
                        .requestMatchers("/api/v1/**").hasAnyRole("SYS_ADMIN",
                                "COM_ADMIN",
                                "SEC_ADMIN",
                                "THR_ADMIN",
                                "EMPLOYEE")
                        .requestMatchers("/api/v1/form/pending").hasAnyRole("SYS_ADMIN",
                                "COM_ADMIN",
                                "SEC_ADMIN",
                                "THR_ADMIN")
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/**").permitAll()
                        .requestMatchers("/api/v1/admin/departments/**", "/api/v1/admin/positions/**").hasAnyRole("COM_ADMIN")
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JwtFilter(tokenProvider, tokenService, redisUtil), UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ 쿠키(credentials) 쓰면 Origin은 반드시 "명시"해야 함
        configuration.setAllowedOrigins(List.of(
                "https://www.bizportal.pro",        // 예: https://bizportal.com
                "http://localhost:3000",
                "https://business.juso.go.kr", // ✅ 추가: 주소 API 비즈니스 도메인
                "https://www.juso.go.kr"
        ));

        configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type","Authorization","X-Requested-With"));
        configuration.setExposedHeaders(List.of("Set-Cookie")); // (필수는 아님, 디버깅에 도움)
        configuration.setAllowCredentials(true);

        // (선택) preflight 캐시
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
