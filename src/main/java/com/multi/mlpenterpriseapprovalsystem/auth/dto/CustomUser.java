package com.multi.mlpenterpriseapprovalsystem.auth.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * 로그인 한 주체 dto
 *
 * @author : 권지영
 * @filename : CustomUser
 * @since : 2025. 12. 17. 수요일
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class CustomUser implements UserDetails {

    private Long subjectId;                 // comNo or empNo
    private TokenSubjectType subjectType;   // COMPANY / EMPLOYEE
    private String comId;                   // EMPLOYEE면 값 있음, COMPANY면 null 가능

    private String username;                // 로그인 식별자(email 또는 comId+empId 등)
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {           // SecurityContext에서 principal 이름으로도 씀
        return username;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}