package com.multi.mlpenterpriseapprovalsystem.organization.positions.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직급 조회 반환 dto
 *
 * @author : 권지영
 * @filename : ResPositionsDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResPositionsDto {

    private Long posNo;
    private String posName;
    private Long empCount;

    @Builder
    public ResPositionsDto(Long posNo, String posName, long empCount) {
        this.posNo = posNo;
        this.posName = posName;
        this.empCount = empCount;
    }
}
