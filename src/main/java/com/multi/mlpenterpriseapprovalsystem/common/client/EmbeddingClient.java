package com.multi.mlpenterpriseapprovalsystem.common.client;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.ReqFastApiProvDeleteDto;
import com.multi.mlpenterpriseapprovalsystem.prov_document.dto.ReqFastApiProvEmbeddingDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 임베딩 요청 클라이언트
 *
 * @author : 김승기
 * @filename : FastApiEmbeddingClient
 * @since : 2025. 12. 29. 월요일
 */
@Slf4j
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

    /**
     * ✅ 규정 삭제 시 FastAPI 벡터(임베딩) 삭제 요청
     * - A(강한 일관성) 정책: 실패하면 예외를 던져서 Spring 트랜잭션이 롤백되게 사용
     */
    public void deleteProvEmbedding(ReqFastApiProvDeleteDto req) {
        log.info("deleteProvEmbedding payload => comId={}, provNo={}", req.getComId(), req.getProvNo());
        fastApiWebClient
                .method(HttpMethod.DELETE)
                .uri("/api/v1/prov-documents/embedding")
                .header("X-CALLBACK-SECRET", callbackSecret)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(req) // DELETE body
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new CustomException(ErrorCode.EMBEDDING_DELETE_FAILED))
                )
                .toBodilessEntity()
                .block();
    }
}