package com.multi.mlpenterpriseapprovalsystem.common.client;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.provdocument.dto.ReqFastApiProvDeleteDto;
import com.multi.mlpenterpriseapprovalsystem.provdocument.dto.ReqFastApiProvEmbeddingDto;
import com.multi.mlpenterpriseapprovalsystem.provdocument.dto.ReqFastApiProvStatusUpdateDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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

    @Value("${internal.ai.prov.callback-url}")
    private String callbackUrlTemplate;

    @Value("${internal.ai.callback-key}")
    private String callbackKey;

    /**
     * ✅ 규정 임베딩 요청 (DTO의 모든 필드 및 콜백 정보 포함)
     */
    public void requestProvEmbedding(ReqFastApiProvEmbeddingDto req) {
        // 1. 콜백 URL 생성
        String callbackUrl = String.format(callbackUrlTemplate, req.getProvNo());

        // 2. 바디 구성 (DTO 필드 + 콜백 보안 정보)
        Map<String, Object> body = new HashMap<>();
        body.put("provNo", req.getProvNo());
        body.put("comId", req.getComId());
        body.put("objectKey", req.getObjectKey());
        body.put("downloadUrl", req.getDownloadUrl());
        body.put("originalName", req.getOriginalName());
        body.put("contentType", req.getContentType());
        body.put("size", req.getSize());
        body.put("isPublic", req.getIsPublic());

        // 콜백 관련 추가 정보
        body.put("callbackUrl", callbackUrl);
        body.put("callbackKey", callbackKey);

        log.info("[AI-Embedding] Requesting embedding for provNo={}, objectKey={}", req.getProvNo(), req.getObjectKey());

        fastApiWebClient.post()
                .uri("/api/v1/prov-documents/embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("[AI-Embedding] Response received for provNo={}, res={}", req.getProvNo(), res))
                .onErrorResume(e -> {
                    log.error("[AI-Embedding] Failed to request embedding for provNo={}", req.getProvNo(), e);
                    return Mono.empty();
                })
                .block(Duration.ofMinutes(1));
    }

    /**
     * ✅ 규정 삭제 요청
     */
    public void deleteProvEmbedding(ReqFastApiProvDeleteDto req) {
        log.info("[AI-Embedding] Delete request => comId={}, provNo={}", req.getComId(), req.getProvNo());

        fastApiWebClient
                .method(HttpMethod.DELETE)
                .uri("/api/v1/prov-documents/embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .map(body -> new CustomException(ErrorCode.EMBEDDING_DELETE_FAILED))
                )
                .toBodilessEntity()
                .block(Duration.ofSeconds(10));
    }

    /**
     * ✅ 공개 상태 업데이트 요청
     */
    public void updateProvStatus(ReqFastApiProvStatusUpdateDto req) {
        log.info("[AI-Embedding] Status update => provNo={}, isPublic={}", req.getProvNo(), req.getIsPublic());

        fastApiWebClient.patch()
                .uri("/api/v1/prov-documents/embedding/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .map(body -> new CustomException(ErrorCode.EMBEDDING_UPDATE_FAILED))
                )
                .toBodilessEntity()
                .block(Duration.ofSeconds(10));
    }
}