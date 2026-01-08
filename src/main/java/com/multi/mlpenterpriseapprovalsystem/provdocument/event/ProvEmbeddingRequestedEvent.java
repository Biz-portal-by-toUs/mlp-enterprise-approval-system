package com.multi.mlpenterpriseapprovalsystem.provdocument.event;

import com.multi.mlpenterpriseapprovalsystem.provdocument.dto.ReqFastApiProvEmbeddingDto;

/**
 * 이벤트 리스너에 들어갈 dto
 *
 * @author : 김승기
 * @filename : ProvEmbeddingRequestedEvent
 * @since : 2026. 1. 8. 목요일
 */
public record ProvEmbeddingRequestedEvent(
        Long provNo,
        ReqFastApiProvEmbeddingDto embeddingReq
) {}