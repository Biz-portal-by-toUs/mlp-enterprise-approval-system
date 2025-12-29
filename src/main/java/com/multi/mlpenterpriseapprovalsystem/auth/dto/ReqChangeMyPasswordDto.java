package com.multi.mlpenterpriseapprovalsystem.auth.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 비밀번호 변경 요청 dto
 *
 * @author : 권지영
 * @filename : ReqChangeMyPasswordDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@NoArgsConstructor
public class ReqChangeMyPasswordDto {

    @NotNull(message = "subjectId는 필수입니다.")
    private Long subjectId;

    @NotNull(message = "subjectType은 필수입니다.")
    private TokenSubjectType subjectType;

    @NotBlank(message = "현재 비밀번호는 필수입니다.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호는 필수입니다.")
    @Pattern(
            regexp = PasswordPolicy.REGEX,
            message = "비밀번호는 8~20자, 대문자/소문자/숫자/특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인은 필수입니다.")
    private String newPasswordConfirm;

    @AssertTrue(message = "새 비밀번호 확인이 일치하지 않습니다.")
    public boolean isNewPasswordConfirmed() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}
