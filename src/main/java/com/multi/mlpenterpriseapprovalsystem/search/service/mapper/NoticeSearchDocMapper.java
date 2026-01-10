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
public class NoticeSearchDocMapper implements SearchDocMapper {

    private final NoticeLoadPort noticeLoadPort;
    private final ObjectMapper om;

    public NoticeSearchDocMapper(NoticeLoadPort noticeLoadPort, @Qualifier("esObjectMapper") ObjectMapper om) {
        this.noticeLoadPort = noticeLoadPort;
        this.om = om;
    }

    @Override
    public SearchDocType supports() {
        return SearchDocType.NOTICE;
    }

    @Override
    public Optional<SearchDocument> buildUpsert(String comId, String sourceId) {
        return noticeLoadPort.load(comId, sourceId)
                .map(n -> {
                    String title = nvl(n.getTitle());

                    // ✅ 기존 정규식 방식 대신 JSON 재귀 파싱 사용
                    String contentText = extractTextFromJson(n.getContents());
                    String summary = makeSummary(contentText, 80);

                    List<String> acl = List.of("COMP:" + comId);

                    return SearchDocument.builder()
                            .docId(docId(comId, supports(), sourceId))
                            .companyId(comId)
                            .type(supports().name())
                            .sourceId(sourceId)
                            .title(title)
                            .content(nvl(n.getContents()))
                            .contentText(contentText)
                            .summary(summary)
                            .url("/notice/" + sourceId)
                            .createdAt(toEpochMillis(n.getCreatedAt()))
                            .updatedAt(toEpochMillis(n.getUpdatedAt()))
                            .acl(acl)
                            .build();
                });
    }

    private String extractTextFromJson(String raw) {
        if (raw == null || raw.isBlank()) return "";
        try {
            Map<String, Object> m = om.readValue(raw, new TypeReference<>() {});
            return recursiveExtractText(m).trim();
        } catch (Exception e) {
            return raw;
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
            for (Object item : (List<?>) obj) {
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

    public interface NoticeLoadPort {
        Optional<NoticeRow> load(String comId, String sourceId);
    }

    @Getter
    @RequiredArgsConstructor
    public static class NoticeRow {
        private final Long noticeNo;
        private final String empId;
        private final String title;
        private final String contents;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
    }
}