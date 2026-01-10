package com.multi.mlpenterpriseapprovalsystem.search.service;

import com.multi.mlpenterpriseapprovalsystem.search.domain.OutboxOp;
import com.multi.mlpenterpriseapprovalsystem.search.domain.OutboxStatus;
import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchDocType;
import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchOutbox;
import com.multi.mlpenterpriseapprovalsystem.search.repository.SearchOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchOutboxAppender
 * @since : 2026. 1. 9. 금요일
 */
@Service
@RequiredArgsConstructor
public class SearchOutboxAppender {

    private final SearchOutboxRepository outboxRepository;

    @Transactional
    public void enqueueUpsert(String comId, SearchDocType type, Object sourceId) {
        outboxRepository.save(SearchOutbox.builder()
                .companyId(comId)
                .docType(type)
                .sourceId(String.valueOf(sourceId))
                .op(OutboxOp.UPSERT)
                .schemaVer(1)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public void enqueueDelete(String comId, SearchDocType type, Object sourceId) {
        outboxRepository.save(SearchOutbox.builder()
                .companyId(comId)
                .docType(type)
                .sourceId(String.valueOf(sourceId))
                .op(OutboxOp.DELETE)
                .schemaVer(1)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .occurredAt(LocalDateTime.now())
                .build());
    }
}