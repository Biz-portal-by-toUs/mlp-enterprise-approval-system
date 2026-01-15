package com.multi.mlpenterpriseapprovalsystem.provdocument.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ai서버에 수정 요청 보낼 dto
 *
 * @author : 김승기
 * @filename : ReqFastApiProvStatusUpdateDto
 * @since : 2026. 1. 15. 목요일
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReqFastApiProvStatusUpdateDto {
    private String comId;
    private Long provNo;
    private Boolean isPublic;
}
