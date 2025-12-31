package com.multi.mlpenterpriseapprovalsystem.auth.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 내정보 가지고 오는 dto
 *
 * @author : 권지영
 * @filename : ResMeDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@AllArgsConstructor
public class ResMeDto {
    private boolean authenticated;
    private TokenSubjectType subjectType; // COMPANY / EMPLOYEE
    private Long subjectId;
    private String username;
}
