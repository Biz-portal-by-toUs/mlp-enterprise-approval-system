package com.multi.mlpenterpriseapprovalsystem.search.service.mapper;

import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.dto.SearchDocument;
import com.multi.mlpenterpriseapprovalsystem.search.service.SearchDocMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MeetingSearchDocMapper implements SearchDocMapper {

    private final MeetingLoadPort meetingLoadPort;

    @Override
    public SearchDocType supports() {
        return SearchDocType.MEETING;
    }

    @Override
    public Optional<SearchDocument> buildUpsert(String comId, String sourceId) {
        return meetingLoadPort.load(comId, Long.parseLong(sourceId))
                .map(m -> {

                    // ✅ contentText: 검색용 본문(ngram/nori 타는 필드)
                    String contentText = buildContentText(m.getAiText(), m.getSttText());

                    // ✅ summary: 리스트용(너희가 summary.ngram도 쓰니까 여기에도 텍스트 넣어두면 좋음)
                    String summary = firstN(nvl(m.getAiText()), 500);

                    // ✅ ACL
                    List<String> acl = buildAcl(comId, m);

                    return SearchDocument.builder()
                            .docId(docId(comId, supports(), sourceId))
                            .companyId(comId)
                            .type(supports().name())
                            .sourceId(sourceId)
                            .title(nvl(m.getTitle()))
                            .summary(summary)
                            .contentText(contentText)                 // ✅ v4 기준 핵심 필드
                            .url("/meeting/" + sourceId)             // ✅ 너희 실제 라우팅에 맞게 수정
                            .createdAt(toEpochMillis(m.getCreatedAt()))
                            .updatedAt(toEpochMillis(m.getUpdatedAt()))
                            .acl(acl)
                            .build();
                });
    }

    private List<String> buildAcl(String comId, MeetingRow m) {
        List<String> acl = new ArrayList<>();

        boolean isPublic = Boolean.TRUE.equals(m.getStatus());
        if (isPublic) {
            // 공개 회의록: 회사 단위로 노출
            acl.add("COMP:" + comId);
            return acl;
        }

        // 비공개: 작성자 + 참가자만
        if (m.getWriterEmpId() != null && !m.getWriterEmpId().isBlank()) {
            acl.add("EMP:" + m.getWriterEmpId());
        }
        if (m.getParticipantEmpIds() != null) {
            for (String empId : m.getParticipantEmpIds()) {
                if (empId != null && !empId.isBlank()) acl.add("EMP:" + empId);
            }
        }
        return acl;
    }

    private String buildContentText(String aiText, String sttText) {
        String a = nvl(aiText).trim();
        String s = nvl(sttText).trim();

        if (a.isEmpty() && s.isEmpty()) return "";

        // 둘 다 있으면 합쳐서 검색되게
        String merged = a.isEmpty() ? s : (s.isEmpty() ? a : (a + "\n\n" + s));

        // ES에는 너무 길어도 되긴 하는데, 안전하게 컷(원하면 더 늘려)
        return firstN(merged, 20000);
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String firstN(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n);
    }

    private long toEpochMillis(LocalDateTime t) {
        if (t == null) return System.currentTimeMillis();
        return t.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
    }

    // ===== Port / DTO =====
    public interface MeetingLoadPort {
        Optional<MeetingRow> load(String comId, Long meetNo);
    }

    @Getter
    @RequiredArgsConstructor
    public static class MeetingRow {
        private final Long meetNo;
        private final String writerEmpId;
        private final String title;
        private final String sttText;
        private final String aiText;
        private final Boolean status;                // 공개/비공개
        private final List<String> participantEmpIds; // meeting_emp 기반
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
    }
}