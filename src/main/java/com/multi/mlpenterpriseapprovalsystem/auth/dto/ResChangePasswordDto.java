package com.multi.mlpenterpriseapprovalsystem.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 비밀번호 변경 응답 dto
 *
 * @author : 권지영
 * @filename : ResChangePasswordDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@AllArgsConstructor
public class ResChangePasswordDto {
    private String message;

    public static ResChangePasswordDto ok() {
        return new ResChangePasswordDto("비밀번호가 변경되었습니다.");
    }
}
