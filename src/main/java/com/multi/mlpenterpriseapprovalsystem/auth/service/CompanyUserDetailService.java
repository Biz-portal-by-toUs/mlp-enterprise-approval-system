package com.multi.mlpenterpriseapprovalsystem.auth.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 로그인 시 회사 정보 가지고 오는 UserDetailService
 *
 * @author : 권지영
 * @filename : CustomUserDetailService
 * @since : 2025. 12. 17. 수요일
 */
@Service
@RequiredArgsConstructor
public class CompanyUserDetailService implements UserDetailsService {

    private final CompanyRepository companyRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Company com = companyRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("회사 없음: " + email));

        return CustomUser.builder()
                .subjectId(com.getComNo()) // 이메일로 쓰면 나중에 뭐 변환해야해서 pk로 사용
                .subjectType(TokenSubjectType.COMPANY)
                .comId(com.getComId())
                .username(com.getEmail())
                .password(com.getPwd())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + com.getRole())))
                .build();
    }
}
