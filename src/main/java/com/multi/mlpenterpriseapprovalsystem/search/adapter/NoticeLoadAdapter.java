package com.multi.mlpenterpriseapprovalsystem.search.adapter;

import com.multi.mlpenterpriseapprovalsystem.search.service.mapper.NoticeSearchDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NoticeLoadAdapter implements NoticeSearchDocMapper.NoticeLoadPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<NoticeSearchDocMapper.NoticeRow> load(String comId, String sourceId) {

        String sql = """
            SELECT
              n.notice_no,
              n.emp_id,
              n.title,
              n.contents,
              n.is_deleted,
              n.created_at,
              n.updated_at
            FROM notice n
            WHERE n.com_id = ?
              AND n.notice_no = ?
              AND (n.is_deleted IS NULL OR n.is_deleted = FALSE)
        """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();

            LocalDateTime createdAt = toLdt(rs.getTimestamp("created_at"));
            LocalDateTime updatedAt = toLdt(rs.getTimestamp("updated_at"));

            return Optional.of(new NoticeSearchDocMapper.NoticeRow(
                    rs.getLong("notice_no"),
                    rs.getString("emp_id"),
                    rs.getString("title"),
                    rs.getString("contents"),
                    createdAt,
                    updatedAt
            ));
        }, comId, sourceId);
    }

    private LocalDateTime toLdt(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}