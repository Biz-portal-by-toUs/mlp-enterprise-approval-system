package com.multi.mlpenterpriseapprovalsystem.search.adapter;

import com.multi.mlpenterpriseapprovalsystem.search.service.mapper.DocumentSearchDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : DocumentLoadAdapter
 * @since : 2026. 1. 9. 금요일
 */
@Component
@RequiredArgsConstructor
public class DocumentLoadAdapter implements DocumentSearchDocMapper.DocumentLoadPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<DocumentSearchDocMapper.DocumentRow> load(String comId, Long docNo) {
        String sql = """
            SELECT
              d.doc_no,
              d.doc_stat,
              d.title,
              d.content,
              d.ai_summ,
              d.created_at,
              d.updated_at
            FROM document d
            WHERE d.com_id = ?
              AND d.doc_no = ?
        """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();

            return Optional.of(new DocumentSearchDocMapper.DocumentRow(
                    rs.getLong("doc_no"),
                    rs.getString("doc_stat"),
                    rs.getString("title"),
                    rs.getString("content"),
                    rs.getString("ai_summ"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
            ));
        }, comId, docNo);
    }
}