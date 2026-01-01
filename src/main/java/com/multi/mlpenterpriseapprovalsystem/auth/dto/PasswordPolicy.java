package com.multi.mlpenterpriseapprovalsystem.auth.dto;

/**
 * 비밀번호 검증
 *
 * @author : 권지영
 * @filename : PasswordPolicy
 * @since : 2025. 12. 29. 월요일
 */
public class PasswordPolicy {
    private PasswordPolicy() {}

    // 특수문자 범위는 프로젝트 정책에 맞게 조정 가능
    public static final String REGEX =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[~!@#$%^&*()_+\\-={}\\[\\]|\\\\:;\"'<>,.?/]).{8,20}$";
}
