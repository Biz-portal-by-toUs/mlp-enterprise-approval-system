package com.multi.mlpenterpriseapprovalsystem.search.service;

import com.multi.mlpenterpriseapprovalsystem.search.domain.OutboxOp;
import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchOutbox;
import com.multi.mlpenterpriseapprovalsystem.search.dto.SearchDocument;
import com.multi.mlpenterpriseapprovalsystem.search.infra.ElasticsearchGateway;
import com.multi.mlpenterpriseapprovalsystem.search.repository.SearchOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchOutboxWorker
 * @since : 2026. 1. 9. 금요일
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableScheduling
public class SearchOutboxWorker {

    private final SearchOutboxRepository outboxRepository;
    private final SearchDocMapperRegistry mapperRegistry;
    private final ElasticsearchGateway es;

    private static final int BATCH_SIZE = 200;
    private static final int MAX_RETRY = 10;

    @Scheduled(fixedDelayString = "${app.search.outbox.fixed-delay-ms:1000}")
    @Transactional
    public void tick() {
        try {
            List<Long> ids = claimIds();
            if (ids.isEmpty()) return;

            List<SearchOutbox> events = outboxRepository.findAllByIds(ids);

            // 처리 리스트 분리
            List<SearchDocument> upserts = new ArrayList<>();
            List<String> deletes = new ArrayList<>();

            for (SearchOutbox e : events) {
                try {
                    handleOne(e, upserts, deletes);
                } catch (Exception ex) {
                    fail(e.getOutboxId(), ex);
                }
            }

            // ES bulk (한방)
            try {
                es.bulkUpsert(upserts);
                es.bulkDelete(deletes);
            } catch (Exception ex) {
                // bulk 자체가 터지면, 해당 배치 전부 재시도 걸어야 함
                String err = stacktrace(ex);
                for (SearchOutbox e : events) fail(e.getOutboxId(), ex);
                log.error("[SEARCH-OUTBOX] bulk failed, all events rescheduled. {}", err);
                return;
            }

            // DONE 마킹
            for (SearchOutbox e : events) {
                outboxRepository.markDone(e.getOutboxId());
            }

        } catch (Exception ex) {
            log.error("[SEARCH-OUTBOX] tick error", ex);
        }
    }

    @Transactional
    protected List<Long> claimIds() {
        List<Long> ids = outboxRepository.claimPendingIdsForUpdate(BATCH_SIZE);
        if (!ids.isEmpty()) outboxRepository.markProcessing(ids);
        return ids;
    }

    private void handleOne(SearchOutbox e, List<SearchDocument> upserts, List<String> deletes) {
        SearchDocType type = e.getDocType();
        String comId = e.getCompanyId();
        String sourceId = e.getSourceId();

        String docId = comId + ":" + type.name() + ":" + sourceId;

        if (e.getOp() == OutboxOp.DELETE) {
            deletes.add(docId);
            return;
        }

        // UPSERT
        SearchDocMapper mapper = mapperRegistry.get(type);
        Optional<SearchDocument> docOpt = mapper.buildUpsert(comId, sourceId);

        if (docOpt.isEmpty()) {
            // 대상이 없거나(삭제됨) or (문서는 FINALIZED만이라서 필터링됨) => ES에서 삭제 처리
            deletes.add(docId);
            return;
        }

        upserts.add(docOpt.get());
    }

    private void fail(Long outboxId, Exception ex) {
        String err = stacktrace(ex);
        // backoff: min(2^retry, 60) minutes
        // retry_count는 DB에서 +1 되므로, 여기서는 현재값을 모르니 nextRetryAt을 보수적으로 2분부터
        LocalDateTime nextRetryAt = LocalDateTime.now().plusMinutes(2);

        outboxRepository.markFailed(outboxId, truncate(err, 8000), nextRetryAt);
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String stacktrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}