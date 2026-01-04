package com.multi.mlpenterpriseapprovalsystem.employee.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메시지 상태 변경 요청 dto
 *
 * @author : 권지영
 * @filename : ReqUpdateMsgStatDto
 * @since : 2026. 1. 4. 일요일
 */
@Getter
@NoArgsConstructor
public class ReqUpdateMsgStatDto {

    private String code;

    public ReqUpdateMsgStatDto(String code) {
        this.code = code;
    }
}
