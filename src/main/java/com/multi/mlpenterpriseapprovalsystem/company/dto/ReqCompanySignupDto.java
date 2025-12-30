package com.multi.mlpenterpriseapprovalsystem.company.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 회사 회원가입 요청 dto
 *
 * @author : 권지영
 * @filename : CompanyReqDto
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@Setter
public class ReqCompanySignupDto {

    // 회사코드: 1~3자리 (영문 대문자/숫자)
    @NotBlank(message = "회사 코드는 필수입니다.")
    @Pattern(regexp = "^[A-Z0-9]{1,3}$", message = "회사 코드는 영문 대문자/숫자 1~3자리여야 합니다.")
    private String comId;

    @NotBlank(message = "회사명은 필수입니다.")
    @Size(max = 50, message = "회사명은 50자 이내여야 합니다.")
    private String comName;

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 50, message = "이메일은 50자 이내여야 합니다.")
    private String email;

    // 비밀번호 정책: 대문자+소문자+숫자+특수문자 포함, 8~20자
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(
            regexp = "^(?=\\S+$)(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
            message = "비밀번호는 8~20자이며, 영문 대문자/소문자/숫자/특수문자를 모두 포함해야 합니다."
    )
    private String pwd;

    // 사업자등록번호: 보통 10자리 숫자 (하이픈 없이)
    @NotBlank(message = "사업자등록번호는 필수입니다.")
    @Pattern(regexp = "^\\d{10}$", message = "사업자등록번호는 숫자 10자리여야 합니다. (하이픈 제외)")
    private String brn;

    @NotBlank(message = "주소는 필수입니다.")
    @Size(max = 100, message = "주소는 100자 이내여야 합니다.")
    private String addr;

    @NotNull(message = "요금제 번호는 필수입니다.")
    private Integer subNo;
}