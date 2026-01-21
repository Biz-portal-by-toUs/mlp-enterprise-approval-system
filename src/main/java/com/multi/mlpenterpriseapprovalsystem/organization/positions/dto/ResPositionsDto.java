package com.multi.mlpenterpriseapprovalsystem.organization.positions.dto;

import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import lombok.*;

/**
 * 직급 조회 반환 dto
 *
 * @author : 권지영
 * @filename : ResPositionsDto
 * @since : 2025. 12. 22. 월요일
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ResPositionsDto {

    private Long posNo;
    private String posName;
    private Integer posOrder;
    private Long empCount;

    public static ResPositionsDto toDto(Positions positions) {
        return ResPositionsDto.builder()
                .posNo(positions.getPosNo())
                .posName(positions.getPosName())
                .posOrder(positions.getPosOrder())
                .build();
    }
}
