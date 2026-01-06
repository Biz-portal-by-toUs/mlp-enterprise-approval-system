package com.multi.mlpenterpriseapprovalsystem.common.config;

import com.multi.mlpenterpriseapprovalsystem.common.jwt.JwtFilter;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

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

                        .requestMatchers("/api/v1/prov-documents/**").hasRole("COM_ADMIN")

                        .requestMatchers("/auth/**",
                                "/meeting-rooms/**",
                                "/admin/**",
                                "/attachment-test",
                                "/schedule/**",
                                "/api/v1/mails/**",
                                "/org-chart/**").permitAll()
                        .requestMatchers(
                                "/uploads/**",
                                "/images/**",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico").permitAll()
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
                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JwtFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 프론트엔드 주소 (현재 localhost 테스트 중이라면 아래와 같이 설정)
        configuration.addAllowedOriginPattern("*");
        // 허용할 HTTP 메서드
        configuration.addAllowedMethod("*");
        // 허용할 헤더 (Authorization 헤더가 포함되어야 함)
        configuration.addAllowedHeader("*");
        // 브라우저가 토큰을 읽을 수 있도록 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
