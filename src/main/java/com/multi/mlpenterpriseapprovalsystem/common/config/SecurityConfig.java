package com.multi.mlpenterpriseapprovalsystem.common.config;

import com.multi.mlpenterpriseapprovalsystem.common.jwt.JwtFilter;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 이렇게 하면 타임리프에서도 세션 사용안하고 jwt 사용하면 됨
                .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::disable) // ✅ 이 줄이 추가되어야 합니다.
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/auth/**",
                                "/meeting-rooms/**").permitAll()
                        .requestMatchers(
                                "/uploads/**",
                                "/images/**",
                                "/css/**",
                                "/js/**",
                                "/favicon.ico").permitAll()
                        .requestMatchers("/api/v1/**").hasAnyRole(    "SYS_ADMIN",
                                "COM_ADMIN",
                                "SEC_ADMIN",
                                "THR_ADMIN",
                                "EMPLOYEE")
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAnyRole("COM_ADMIN")
                        .anyRequest().authenticated()
                )

                .addFilterBefore(new JwtFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();

    }
}
