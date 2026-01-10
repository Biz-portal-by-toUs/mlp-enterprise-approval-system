package com.multi.mlpenterpriseapprovalsystem.search.service.mapper;

import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.dto.SearchDocument;
import com.multi.mlpenterpriseapprovalsystem.search.service.SearchDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DocumentSearchDocMapper implements SearchDocMapper {

    private final DocumentLoadPort documentLoadPort;

    @Override
    public SearchDocType supports() {
        return SearchDocType.APPROVAL;
    }

    @Override
    public Optional<SearchDocument> buildUpsert(String comId, String sourceId) {
        return documentLoadPort.load(comId, Long.parseLong(sourceId))
                .filter(d -> "FI".equalsIgnoreCase(nvl(d.getStatus()))) // ✅ FI만 노출
                .map(d -> {
                    String title = nvl(d.getTitle());
                    String content = nvl(d.getContent());
                    String aiSummary = nvl(d.getSummary()); // ai_summ

                    // ✅ 검색/하이라이트용: content 기반 (aiSumm는 summary로만)
                    String contentText = content;

                    // ✅ summary: ai_summ가 있으면 그걸 쓰고, 없으면 content 앞부분
                    String summary = !aiSummary.isBlank() ? aiSummary : makeSummary(contentText, 80);

                    return SearchDocument.builder()
                            .docId(docId(comId, supports(), sourceId))
                            .companyId(comId)
                            .type(supports().name())
                            .sourceId(sourceId)
                            .title(title)

                            // (선택) 원본 content 저장
                            .content(content)

                            // ✅ 핵심
                            .contentText(contentText)

                            .summary(summary)
                            .url("/documents/" + sourceId + "?status=FINALIZED") // API 파라미터는 FINALIZED 유지
                            .createdAt(toEpochMillis(d.getCreatedAt()))
                            .updatedAt(toEpochMillis(d.getUpdatedAt()))
                            .acl(List.of("COMP:" + comId))
                            .build();
                });
    }

    private String makeSummary(String text, int maxLen) {
        if (text == null) return "";
        String t = text.trim();
        if (t.isEmpty()) return "";
        if (t.length() <= maxLen) return t;
        return t.substring(0, maxLen) + "...";
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private long toEpochMillis(LocalDateTime t) {
        if (t == null) return System.currentTimeMillis();
        return t.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
    }

    // ====== Port / DTO ======
    public interface DocumentLoadPort {
        Optional<DocumentRow> load(String comId, Long docNo);
    }

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    public static class DocumentRow {
        private final Long docNo;
        private final String status;     // doc_stat (FI 등)
        private final String title;
        private final String content;
        private final String summary;    // ai_summ
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
    }
}