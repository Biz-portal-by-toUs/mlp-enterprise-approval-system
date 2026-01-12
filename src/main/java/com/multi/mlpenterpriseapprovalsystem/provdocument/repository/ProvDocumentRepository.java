package com.multi.mlpenterpriseapprovalsystem.provdocument.repository;

import com.multi.mlpenterpriseapprovalsystem.provdocument.domain.ProvDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사내 규정 repository
 *
 * @author : 김승기
 * @filename : ProvDocumentRepository
 * @since : 2025. 12. 29. 월요일
 */
public interface ProvDocumentRepository extends JpaRepository<ProvDocument, Long> {
    // ProvDocument 엔티티의 procStat와 updatedAt 기준
    List<ProvDocument> findAllByProcStatAndUpdatedAtBefore(String procStat, LocalDateTime threshold);
    @Query("""
        SELECT d FROM ProvDocument d
        WHERE d.company.comId = :comId
          AND (:isPublic IS NULL OR d.isPublic = :isPublic)
          AND (:keyword IS NULL OR :keyword = '' OR LOWER(d.docTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<ProvDocument> searchDocuments(
            @Param("comId") String comId,
            @Param("keyword") String keyword,
            @Param("isPublic") Boolean isPublic,
            Pageable pageable
    );

    @Query("""
        SELECT d FROM ProvDocument d
        WHERE d.company.comId = :comId
          AND d.isPublic = true
          AND d.procStat = 'DONE'
    """)
    Page<ProvDocument> findChatbotAvailableDocuments(
            @Param("comId") String comId,
            Pageable pageable
    );
}
