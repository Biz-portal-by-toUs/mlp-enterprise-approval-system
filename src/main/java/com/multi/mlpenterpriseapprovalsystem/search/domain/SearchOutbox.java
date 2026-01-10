package com.multi.mlpenterpriseapprovalsystem.search.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchOutbox
 * @since : 2026. 1. 9. 금요일
 */
@Entity
@Table(
        name = "search_outbox",
        indexes = {
                @Index(name = "ix_outbox_pick", columnList = "status,next_retry_at,occurred_at"),
                @Index(name = "ix_outbox_doc", columnList = "company_id,doc_type,source_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SearchOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outbox_id")
    private Long outboxId;

    @Column(name = "company_id", nullable = false, length = 20)
    private String companyId; // comId

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 30)
    private SearchDocType docType;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "op", nullable = false, length = 10)
    private OutboxOp op;

    @Column(name = "schema_ver", nullable = false)
    private int schemaVer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public void markProcessing() {
        this.status = OutboxStatus.PROCESSING;
        this.lastError = null;
    }

    public void markDone() {
        this.status = OutboxStatus.DONE;
        this.processedAt = LocalDateTime.now();
        this.lastError = null;
        this.nextRetryAt = null;
    }

    public void markFailed(String error, LocalDateTime nextRetryAt) {
        this.status = OutboxStatus.PENDING;
        this.retryCount += 1;
        this.lastError = error;
        this.nextRetryAt = nextRetryAt;
    }
}