package com.multi.mlpenterpriseapprovalsystem.document.dto.res;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenAi에 문서내용 요약 응답 Dto
 *
 * @author : 이지헌
 * @filename : DocumentOpenAiResponse
 * @since : 25. 12. 28. 일요일
 */
@Getter
@NoArgsConstructor
public class ResDocOpenAiDto {

    private List<Choice> choices;

    @Getter
    @NoArgsConstructor
    public static class Choice {
        private Message message;
    }

    @Getter
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
