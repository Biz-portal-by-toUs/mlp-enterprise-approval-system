package com.multi.mlpenterpriseapprovalsystem.provdocument.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * fastAPI로 vectorDB 삭제 요청 보내는 dto
 *
 * @author : 김승기
 * @filename : ReqFastApiProvDeleteDto
 * @since : 2025. 12. 31. 수요일
 */
@Getter
@Builder
public class ReqFastApiProvDeleteDto {
    private String comId;
    private Long provNo;
}