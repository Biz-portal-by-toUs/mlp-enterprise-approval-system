package com.multi.mlpenterpriseapprovalsystem.document.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAi에 문서내용 요약 요청 Dto
 *
 * @author : 이지헌
 * @filename : DocumentOpenAiRequest
 * @since : 25. 12. 28. 일요일
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqDocOpenAiDto {

    private String model;

    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
