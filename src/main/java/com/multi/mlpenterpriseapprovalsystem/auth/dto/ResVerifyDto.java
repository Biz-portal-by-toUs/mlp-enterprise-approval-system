package com.multi.mlpenterpriseapprovalsystem.auth.dto;

import com.multi.mlpenterpriseapprovalsystem.common.enums.TokenSubjectType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 인증 반환 dto
 *
 * @author : 권지영
 * @filename : ResVerifyDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@Setter
@AllArgsConstructor
public class ResVerifyDto {

    private Long SubjectId;
    private TokenSubjectType SubjectType;
}
