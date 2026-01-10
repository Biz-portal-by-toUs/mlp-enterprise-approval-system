package com.multi.mlpenterpriseapprovalsystem.search.backfill;

import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.service.SearchOutboxAppender;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchBackfillService
 * @since : 2026. 1. 9. 금요일
 */
@Service
@RequiredArgsConstructor
public class SearchBackfillService {

    private final JdbcTemplate jdbc;
    private final SearchOutboxAppender outbox; // 아래에 같이 제공

    private static final int PAGE_SIZE = 500;

    @Transactional
    public Map<String, Object> backfillAll(String comId) {
        long b = backfillBoards(comId);
        long n = backfillNotices(comId);          // ✅ 추가
        long s = backfillSchedules(comId);
        long d = backfillDocumentsFinalizedOnly(comId);
        long m = backfillMeetings(comId);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("companyId", comId);
        res.put("boards", b);
        res.put("notices", n);                    // ✅ 추가
        res.put("schedules", s);
        res.put("documentsFinalizedOnly", d);
        res.put("meetings", m);
        return res;
    }

    private long backfillNotices(String comId) {
        String sql = """
        SELECT n.notice_no
        FROM notice n
        WHERE n.com_id = ?
          AND (n.is_deleted IS NULL OR n.is_deleted = 0)
        ORDER BY n.notice_no
    """;

        List<Long> ids = jdbc.query(sql, (rs, i) -> rs.getLong(1), comId);
        for (Long id : ids) {
            // ✅ NoticeSearchDocMapper supports()가 BOARD면 반드시 BOARD로 enqueue해야 함
            outbox.enqueueUpsert(comId, SearchDocType.NOTICE, String.valueOf(id));
        }
        return ids.size();
    }

    private long backfillBoards(String comId) {
        String sql = """
            SELECT b.board_no
            FROM board b
            WHERE b.com_id = ?
              AND (b.is_deleted IS NULL OR b.is_deleted = 0)
            ORDER BY b.board_no
        """;

        List<Long> ids = jdbc.query(sql, (rs, i) -> rs.getLong(1), comId);
        for (Long id : ids) {
            outbox.enqueueUpsert(comId, SearchDocType.BOARD, String.valueOf(id));
        }
        return ids.size();
    }

    private long backfillSchedules(String comId) {
        String sql = """
            SELECT s.sch_no
            FROM schedule s
            WHERE s.com_id = ?
            ORDER BY s.sch_no
        """;

        List<Long> ids = jdbc.query(sql, (rs, i) -> rs.getLong(1), comId);
        for (Long id : ids) {
            outbox.enqueueUpsert(comId, SearchDocType.SCHEDULE, String.valueOf(id));
        }
        return ids.size();
    }

    private long backfillDocumentsFinalizedOnly(String comId) {
        String sql = """
            SELECT d.doc_no
            FROM document d
            WHERE d.com_id = ?
              AND d.doc_stat = 'FI'
            ORDER BY d.doc_no
        """;

        List<Long> ids = jdbc.query(sql, (rs, i) -> rs.getLong(1), comId);
        for (Long id : ids) {
            outbox.enqueueUpsert(comId, SearchDocType.APPROVAL, String.valueOf(id));
        }
        return ids.size();
    }


    /**
     * ✅ Meeting backfill
     * - meeting 테이블: meet_no, com_id, is_deleted 사용
     * - (선택) 공개/비공개(status) 정책이 있으면 WHERE에 추가하면 됨
     */
    private long backfillMeetings(String comId) {
        String sql = """
            SELECT m.meet_no
            FROM meeting m
            WHERE m.com_id = ?
              AND (m.is_deleted IS NULL OR m.is_deleted = 0)
            ORDER BY m.meet_no
        """;

        List<Long> ids = jdbc.query(sql, (rs, i) -> rs.getLong(1), comId);
        for (Long id : ids) {
            outbox.enqueueUpsert(comId, SearchDocType.MEETING, String.valueOf(id));
        }
        return ids.size();
    }
}