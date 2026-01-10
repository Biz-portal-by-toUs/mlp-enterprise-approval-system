package com.multi.mlpenterpriseapprovalsystem.search.adapter;

import com.multi.mlpenterpriseapprovalsystem.search.service.mapper.MeetingSearchDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MeetingLoadAdapter implements MeetingSearchDocMapper.MeetingLoadPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<MeetingSearchDocMapper.MeetingRow> load(String comId, Long meetNo) {

        String sql = """
            SELECT
              m.meet_no,
              m.title,
              m.stt_text,
              m.ai_text,
              m.status,
              m.is_deleted,
              m.emp_id     AS writer_emp_id,
              m.created_at,
              m.updated_at
            FROM meeting m
            WHERE m.com_id = ?
              AND m.meet_no = ?
        """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();

            boolean isDeleted = rs.getBoolean("is_deleted");
            if (isDeleted) return Optional.empty(); // ✅ 삭제면 검색 제외(ES delete 유도)

            Long id = rs.getLong("meet_no");

            // 참가자(사원) 목록
            List<String> participantEmpIds = jdbc.query(
                    "SELECT me.emp_id FROM meeting_emp me WHERE me.meet_no = ?",
                    (r2, i) -> r2.getString("emp_id"),
                    id
            );

            LocalDateTime createdAt = toLdt(rs.getTimestamp("created_at"));
            LocalDateTime updatedAt = toLdt(rs.getTimestamp("updated_at"));

            return Optional.of(new MeetingSearchDocMapper.MeetingRow(
                    id,
                    rs.getString("writer_emp_id"),
                    rs.getString("title"),
                    rs.getString("stt_text"),
                    rs.getString("ai_text"),
                    (Boolean) rs.getObject("status"),   // status는 nullable일 수 있어서 getObject
                    participantEmpIds,
                    createdAt,
                    updatedAt
            ));
        }, comId, meetNo);
    }

    private LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}