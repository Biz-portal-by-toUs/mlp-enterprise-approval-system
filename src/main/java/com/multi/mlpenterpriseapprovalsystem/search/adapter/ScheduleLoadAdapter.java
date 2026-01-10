package com.multi.mlpenterpriseapprovalsystem.search.adapter;

import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope;
import com.multi.mlpenterpriseapprovalsystem.search.service.mapper.ScheduleSearchDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ScheduleLoadAdapter implements ScheduleSearchDocMapper.ScheduleLoadPort {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<ScheduleSearchDocMapper.ScheduleRow> load(String comId, String sourceId) {
        Long schNo;
        try {
            schNo = Long.parseLong(sourceId);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        String sql = """
            SELECT
              s.sch_no,
              s.scope,
              s.reg_emp,
              s.dep_no,
              s.title,
              s.content,
              s.created_at,
              s.updated_at
            FROM schedule s
            WHERE s.com_id = ?
              AND s.sch_no = ?
            """;

        List<ScheduleSearchDocMapper.ScheduleRow> rows = jdbc.query(sql, (rs, rowNum) -> {
            String scopeStr = rs.getString("scope");
            CalendarScope scope = (scopeStr == null || scopeStr.isBlank())
                    ? CalendarScope.PERSONAL
                    : CalendarScope.valueOf(scopeStr);

            Long depNo = (rs.getObject("dep_no") == null) ? null : rs.getLong("dep_no");

            return new ScheduleSearchDocMapper.ScheduleRow(
                    rs.getLong("sch_no"),
                    scope,
                    rs.getString("reg_emp"),          // ownerEmpId
                    depNo,
                    rs.getString("title"),
                    rs.getString("content"),
                    toLocalDateTime(rs.getTimestamp("created_at")),
                    toLocalDateTime(rs.getTimestamp("updated_at"))
            );
        }, comId, schNo);

        return rows.stream().findFirst();
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}