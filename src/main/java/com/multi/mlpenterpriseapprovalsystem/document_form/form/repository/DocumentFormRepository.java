package com.multi.mlpenterpriseapprovalsystem.document_form.form.repository;

import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.*;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.enums.DocumentFormStats;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.DocumentFormListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormRepository
 * @since : 2025-12-22 월요일
 */
public interface DocumentFormRepository extends JpaRepository<DocumentForm, Long> {

    Page<DocumentForm> findByDocfoStatOrderByDocfoNoAsc(DocumentFormStats docfoStat, Pageable pageable);

    @Query("""
        select new com.multi.mlpenterpriseapprovalsystem.document_form.form.dto.res.DocumentFormListResDto(
            d.docfoNo,
            d.docfoName,
            d.docfoStat
        )
          from DocumentForm d
         where d.docfoStat = :stat
         order by d.docfoNo asc
    """)
    Page<DocumentFormListResDto> findListByDocfoStat(@Param("stat") DocumentFormStats stat,
                                                     Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DocumentForm d
           set d.docfoStat = :stat
         where d.docfoNo = :docfoNo
    """)
    int updateDocfoStat(@Param("docfoNo") Long docfoNo,
                        @Param("stat") DocumentFormStats stat);
}
