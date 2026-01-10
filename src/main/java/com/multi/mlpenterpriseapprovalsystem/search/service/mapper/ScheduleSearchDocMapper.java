package com.multi.mlpenterpriseapprovalsystem.search.service.mapper;

import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope;
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
public class ScheduleSearchDocMapper implements SearchDocMapper {

    private final ScheduleLoadPort scheduleLoadPort;

    @Override
    public SearchDocType supports() {
        return SearchDocType.SCHEDULE;
    }

    @Override
    public Optional<SearchDocument> buildUpsert(String comId, String sourceId) {
        return scheduleLoadPort.load(comId, sourceId)
                .flatMap(s -> {
                    CalendarScope scope = (s.getScope() == null) ? CalendarScope.PERSONAL : s.getScope();

                    // ✅ ACL 설정: scope에 따라 권한 분기
                    List<String> acl = new ArrayList<>(2);

                    // 개인 일정은 "소유자"가 반드시 있어야 함
                    String ownerEmpId = nvl(s.getOwnerEmpId()).trim();
                    if (!ownerEmpId.isEmpty()) {
                        acl.add("EMP:" + ownerEmpId);
                    }

                    if (scope == CalendarScope.DEPARTMENT) {
                        if (s.getDepNo() == null) {
                            // dep_no 없으면 접근 범위가 애매해서 인덱싱 자체를 스킵(권장)
                            return Optional.empty();
                        }
                        acl.add("DEP:" + s.getDepNo());
                    } else if (scope == CalendarScope.COMPANY) {
                        // 회사 일정: 회사원 전체
                        // ⚠️ 여기 prefix(COM/COMP)는 프로젝트 인가 로직이 쓰는 것과 반드시 맞춰라
                        acl.add("COMP:" + comId);
                    } else {
                        // PERSONAL: EMP만
                        if (acl.isEmpty()) {
                            // ownerEmpId가 없으면 개인 일정 문서로서 의미가 없음 → 스킵
                            return Optional.empty();
                        }
                    }

                    String title = nvl(s.getTitle());
                    String content = nvl(s.getContent());

                    // ✅ 검색 품질: title + content 합쳐서 인덱싱
                    String contentText = buildContentText(title, content);

                    // ✅ 요약
                    String summary = makeSummary(content, 80);

                    return Optional.of(
                            SearchDocument.builder()
                                    .docId(docId(comId, supports(), sourceId))
                                    .companyId(comId)
                                    .type(supports().name())
                                    .sourceId(sourceId)
                                    .title(title)
                                    .content(content)
                                    .contentText(contentText)
                                    .summary(summary)
                                    .url("/schedule/calendar?open=1&schNo=" + sourceId + "&scope=" + scope.name())
                                    .createdAt(toEpochMillis(s.getCreatedAt()))
                                    .updatedAt(toEpochMillis(s.getUpdatedAt()))
                                    .acl(acl)
                                    .scope(s.getScope().name())
                                    .build()
                    );
                });
    }

    private String buildContentText(String title, String content) {
        String t = nvl(title).trim();
        String c = nvl(content).trim();
        if (t.isEmpty()) return c;
        if (c.isEmpty()) return t;
        return t + "\n" + c;
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
    public interface ScheduleLoadPort {
        Optional<ScheduleRow> load(String comId, String sourceId);
    }

    @Getter
    @RequiredArgsConstructor
    public static class ScheduleRow {
        private final Long schNo;
        private final CalendarScope scope;
        private final String ownerEmpId;  // PERSONAL일 때 소유자 (보통 reg_emp)
        private final Long depNo;
        private final String title;
        private final String content;
        private final LocalDateTime createdAt;
        private final LocalDateTime updatedAt;
    }
}