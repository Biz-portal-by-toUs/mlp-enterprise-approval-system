package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자용 사원 정보 수정 요청 dto
 *
 * @author : 권지영
 * @filename : ReqAdminEmployeeUpdateDto
 * @since : 2025. 12. 28. 일요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReqAdminEmployeeUpdateDto {

    @NotNull
    private Long depNo;

    @NotNull
    private Long posNo;

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank
    @Pattern(
            regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
            message = "전화번호 형식이 올바르지 않습니다. 예) 010-1234-5678"
    )
    private String phone;

    @NotBlank
    @Size(max = 20)
    private String workPhone;

    @NotBlank
    @Size(max = 100)
    private String addr;

    @NotBlank
    @Pattern(regexp = "^(M|F)$", message = "gen은 M/F만 가능합니다.")
    private String gen;

    @NotNull
    private RoleType role;
}
