package com.multi.mlpenterpriseapprovalsystem.common.client;

import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.ReqFastApiProvEmbeddingDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 임베딩 요청 클라이언트
 *
 * @author : 김승기
 * @filename : FastApiEmbeddingClient
 * @since : 2025. 12. 29. 월요일
 */
@Component
public class EmbeddingClient {

    private final WebClient fastApiWebClient;

    public EmbeddingClient(@Qualifier("fastApiWebClient") WebClient fastApiWebClient) {
        this.fastApiWebClient = fastApiWebClient;
    }

    @Value("${ai.fastapi.callback-secret}")
    private String callbackSecret;

    public void requestProvEmbedding(ReqFastApiProvEmbeddingDto req) {
        fastApiWebClient.post()
                .uri("/api/v1/prov-documents/embedding") // FastAPI에서 만들 엔드포인트
                .header("X-CALLBACK-SECRET", callbackSecret)
                .bodyValue(req)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}