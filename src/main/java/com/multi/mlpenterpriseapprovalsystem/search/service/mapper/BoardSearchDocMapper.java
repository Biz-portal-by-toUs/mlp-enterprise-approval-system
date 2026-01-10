package com.multi.mlpenterpriseapprovalsystem.search.service.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.dto.SearchDocument;
import com.multi.mlpenterpriseapprovalsystem.search.service.SearchDocMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class BoardSearchDocMapper implements SearchDocMapper {

    private final BoardLoadPort boardLoadPort;

    @Qualifier("esObjectMapper")
    private final ObjectMapper om;

    public BoardSearchDocMapper(BoardLoadPort boardLoadPort, @Qualifier("esObjectMapper") ObjectMapper om) {
        this.boardLoadPort = boardLoadPort;
        this.om = om;
    }

    @Override
    public SearchDocType supports() {
        return SearchDocType.BOARD;
    }

    @Override
    public Optional<SearchDocument> buildUpsert(String comId, String sourceId) {
        return boardLoadPort.load(comId, Long.parseLong(sourceId))
                .filter(b -> !Boolean.TRUE.equals(b.getIsDeleted()))
                .map(b -> {
                    String title = nvl(b.getTitle());
                    String rawContent = nvl(b.getContent());

                    // ✅ TipTap JSON에서 모든 text 재귀 추출
                    String contentText = extractTextFromBoardContent(rawContent);
                    String summary = makeSummary(contentText, 80);

                    return SearchDocument.builder()
                            .docId(docId(comId, supports(), sourceId))
                            .companyId(comId)
                            .type(supports().name())
                            .sourceId(sourceId)
                            .title(title)
                            .content(rawContent)
                            .contentText(contentText) // 검색용 텍스트
                            .summary(summary)
                            .url("/board/" + sourceId)
                            .createdAt(toEpochMillis(b.getCreatedAt()))
                            .updatedAt(toEpochMillis(b.getUpdatedAt()))
                            .acl(List.of("COMP:" + comId))
                            .build();
                });
    }

    /**
     * TipTap JSON 구조에서 재귀적으로 모든 text 필드를 합칩니다.
     */
    private String extractTextFromBoardContent(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            Map<String, Object> m = om.readValue(raw, new TypeReference<>() {});
            return recursiveExtractText(m).trim();
        } catch (Exception e) {
            return raw; // JSON이 아니면 원문 반환
        }
    }

    private String recursiveExtractText(Object obj) {
        StringBuilder sb = new StringBuilder();
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            if (map.containsKey("text")) {
                sb.append(map.get("text")).append(" ");
            }
            if (map.containsKey("content")) {
                sb.append(recursiveExtractText(map.get("content")));
            }
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            for (Object item : list) {
                sb.append(recursiveExtractText(item));
            }
        }
        return sb.toString();
    }

    private String makeSummary(String text, int maxLen) {
        if (text == null) return "";
        String t = text.trim();
        if (t.isEmpty()) return "";
        return (t.length() <= maxLen) ? t : t.substring(0, maxLen) + "...";
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private long toEpochMillis(LocalDateTime t) {
        if (t == null) return System.currentTimeMillis();
        return t.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
    }

    public interface BoardLoadPort {
        Optional<BoardRow> load(String comId, Long boardNo);
    }

    @Getter
    @RequiredArgsConstructor
    public static class BoardRow {
        private final Long boardNo;
        private final String title;
        private final String content;
        private final Boolean isDeleted;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
    }
}