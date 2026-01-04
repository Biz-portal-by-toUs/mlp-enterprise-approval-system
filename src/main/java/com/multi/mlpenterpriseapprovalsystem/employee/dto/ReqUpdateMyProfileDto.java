package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 정보 수정 요청 dto
 *
 * @author : 권지영
 * @filename : ReqUpdateMyProfileDto
 * @since : 2026. 1. 4. 일요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReqUpdateMyProfileDto {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 50, message = "이메일은 50자 이하여야 합니다.")
    private String email;

    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
    private String phone;

    @Size(max = 10, message = "사내번호는 10자 이하여야 합니다.")
    private String workPhone;

    @Size(max = 100, message = "주소는 100자 이하여야 합니다.")
    private String addr;
}
