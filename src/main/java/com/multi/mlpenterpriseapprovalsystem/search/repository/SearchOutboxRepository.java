package com.multi.mlpenterpriseapprovalsystem.search.repository;

import com.multi.mlpenterpriseapprovalsystem.search.domain.SearchOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : SearchOutboxRepository
 * @since : 2026. 1. 9. 금요일
 */
public interface SearchOutboxRepository extends JpaRepository<SearchOutbox, Long> {

    /**
     * MySQL 8: FOR UPDATE SKIP LOCKED 지원
     * - 트랜잭션 안에서 호출해야 함
     */
    @Query(value = """
            SELECT outbox_id
            FROM search_outbox
            WHERE status = 'PENDING'
              AND (next_retry_at IS NULL OR next_retry_at <= NOW())
            ORDER BY occurred_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> claimPendingIdsForUpdate(@Param("limit") int limit);

    @Modifying
    @Query(value = """
            UPDATE search_outbox
            SET status = 'PROCESSING', last_error = NULL
            WHERE outbox_id IN (:ids)
            """, nativeQuery = true)
    int markProcessing(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = """
            UPDATE search_outbox
            SET status = 'DONE', processed_at = NOW(), last_error = NULL, next_retry_at = NULL
            WHERE outbox_id = :id
            """, nativeQuery = true)
    int markDone(@Param("id") Long id);

    @Modifying
    @Query(value = """
            UPDATE search_outbox
            SET status = 'PENDING',
                retry_count = retry_count + 1,
                last_error = :err,
                next_retry_at = :nextRetryAt
            WHERE outbox_id = :id
            """, nativeQuery = true)
    int markFailed(@Param("id") Long id,
                   @Param("err") String err,
                   @Param("nextRetryAt") LocalDateTime nextRetryAt);

    @Transactional(readOnly = true)
    @Query("select o from SearchOutbox o where o.outboxId in :ids")
    List<SearchOutbox> findAllByIds(@Param("ids") List<Long> ids);
}