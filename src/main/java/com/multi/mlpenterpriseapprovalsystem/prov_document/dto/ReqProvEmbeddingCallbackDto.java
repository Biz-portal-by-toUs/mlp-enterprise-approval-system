package com.multi.mlpenterpriseapprovalsystem.prov_document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 임베딩 콜백 reqDto (성공시 청크 카운트 포함)
 *
 * @author : 김승기
 * @filename : ReqProvAiCallbackDto
 * @since : 2025. 12. 29. 월요일
 */
@Getter
@Setter
public class ReqProvEmbeddingCallbackDto {

    @NotNull
    private Long provNo;

    @NotNull
    private Boolean success;

    private Integer chunkCnt;

    private String errorMsg;
}