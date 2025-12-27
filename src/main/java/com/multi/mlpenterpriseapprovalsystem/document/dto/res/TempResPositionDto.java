package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import lombok.Builder;
import lombok.Data;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : TempResPositionDto
 * @since : 25. 12. 22. 월요일
 */
@Data
@Builder
public class TempResPositionDto {
    private Long posNo;
    private String posName;
    private int posOrder;

    public static TempResPositionDto toDto(Positions positions) {
        return TempResPositionDto.builder()
                .posNo(positions.getPosNo())
                .posName(positions.getPosName())
                .posOrder(positions.getPosOrder())
                .build();
    }
}
