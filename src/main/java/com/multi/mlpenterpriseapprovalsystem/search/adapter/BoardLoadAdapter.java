package com.multi.mlpenterpriseapprovalsystem.search.adapter;

import com.multi.mlpenterpriseapprovalsystem.search.service.mapper.BoardSearchDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : BoardLoadAdapter
 * @since : 2026. 1. 9. 금요일
 */
@Component
@RequiredArgsConstructor
public class BoardLoadAdapter implements BoardSearchDocMapper.BoardLoadPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<BoardSearchDocMapper.BoardRow> load(String comId, Long boardNo) {
        String sql = """
            SELECT
              b.board_no,
              b.title,
              b.contents,
              b.is_deleted,
              b.created_at,
              b.updated_at
            FROM board b
            WHERE b.com_id = ?
              AND b.board_no = ?
        """;

        return jdbc.query(sql, rs -> {
            if (!rs.next()) return Optional.empty();

            return Optional.of(new BoardSearchDocMapper.BoardRow(
                    rs.getLong("board_no"),
                    rs.getString("title"),
                    rs.getString("contents"), // ✅ content로 사용
                    rs.getBoolean("is_deleted"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
            ));
        }, comId, boardNo);
    }
}