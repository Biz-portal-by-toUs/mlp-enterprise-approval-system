package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 관리자용 사원 등록 요청 dto
 *
 * @author : 권지영
 * @filename : ReqAdminEmployeeCreateDto
 * @since : 2025. 12. 28. 일요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReqAdminEmployeeCreateDto {

    @NotNull
    private Long depNo;

    @NotNull
    private Long posNo;

    @NotBlank
    @Size(max = 20)
    private String empName;

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank
    @Size(max = 15)
    private String phone;

    @NotBlank
    @Size(max = 10)
    private String workPhone;

    @NotBlank
    @Size(max = 100)
    private String addr;

    @NotBlank
    @Size(max = 1)
    private String gen;

    @NotNull
    private RoleType role;

    @NotNull
    private LocalDate hireDate;

    private String objectKey;

    @NotNull
    private LocalDate birth;
}
