package com.multi.mlpenterpriseapprovalsystem.provdocument.event;

import com.multi.mlpenterpriseapprovalsystem.common.client.EmbeddingClient;
import com.multi.mlpenterpriseapprovalsystem.provdocument.service.ProvDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사내 규정 임베딩 등록 테스트 이벤트 리스너
 *
 * @author : 김승기
 * @filename : ProvDocumentEventListener
 * @since : 2026. 1. 8. 목요일
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProvDocumentEventListener {

    private final EmbeddingClient embeddingClient;
    private final ProvDocumentService provDocumentService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ProvEmbeddingRequestedEvent e) {
        try {
            // 이벤트에 담겨온 DTO를 그대로 사용
            embeddingClient.requestProvEmbedding(e.embeddingReq());
            log.info("[EMBEDDING REQUEST SUCCESS] provNo={}", e.provNo());
        } catch (Exception ex) {
            log.error("[EMBEDDING REQUEST FAILED] provNo={}, reason={}", e.provNo(), ex.getMessage());

            // 네트워크 오류나 서버 다운 시 실패 상태 기록
            provDocumentService.handleEmbeddingFailure(e.provNo(), "AI 서버 연결 실패: " + ex.getMessage());
        }
    }
}