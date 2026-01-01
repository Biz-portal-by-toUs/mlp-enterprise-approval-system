package com.multi.mlpenterpriseapprovalsystem.document_form.form.repository;

import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.ResDocumentFormListDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormRepository
 * @since : 2025-12-22 월요일
 */

public interface DocumentFormRepository extends JpaRepository<DocumentForm, Long> {

    Page<DocumentForm> findByDocfoStatAndCompany_ComIdOrderByDocfoNoAsc(
            DocumentFormStats stat,
            String comId,
            Pageable pageable
    );

    Page<DocumentForm> findByDocfoStatAndCompany_ComIdAndDocfoNameContainingIgnoreCaseOrderByDocfoNoAsc(
            DocumentFormStats stat,
            String comId,
            String docfoName,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DocumentForm f
           set f.docfoStat = :stat,
               f.rejectReason = :reason
         where f.docfoNo = :docfoNo
    """)
    int updateStatusAndReason(@Param("docfoNo") Long docfoNo,
                              @Param("stat") DocumentFormStats stat,
                              @Param("reason") String reason);

    Page<DocumentForm> findByDocfoStatInAndCompany_ComIdOrderByDocfoNoAsc(
            java.util.List<DocumentFormStats> stats,
            String comId,
            Pageable pageable
    );

    Page<DocumentForm> findByDocfoStatInAndCompany_ComIdAndDocfoNameContainingIgnoreCaseOrderByDocfoNoAsc(
            java.util.List<DocumentFormStats> stats,
            String comId,
            String docfoName,
            Pageable pageable
    );
}