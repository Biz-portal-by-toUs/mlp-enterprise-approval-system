package com.multi.mlpenterpriseapprovalsystem.documentform.form.repository;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.enums.DocumentFormStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

/**
 * 문서 양식 관리 repository
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

    @Modifying
    @Query("""
            update DocumentForm f
               set f.docfoName = :name,
                   f.cnttJson = :json,
                   f.cnttHtml = :html,
                   f.docfoStat = :stat,
                   f.rejectReason = null
             where f.docfoNo = :docfoNo
            """)
    int updateDraft(
            @Param("docfoNo") Long docfoNo,
            @Param("name") String name,
            @Param("json") String json,
            @Param("html") String html,
            @Param("stat") DocumentFormStats stat
    );

    @Query("""
                select f
                  from DocumentForm f
                 where f.company.comId = :comId
                   and f.writer.empId = :writerId
                   and f.docfoStat = :stat
                 order by f.docfoNo desc
            """)
    Page<DocumentForm> findMyByStat(
            @Param("comId") String comId,
            @Param("writerId") String writerId,
            @Param("stat") DocumentFormStats stat,
            Pageable pageable
    );

    @Query("""
                select f
                  from DocumentForm f
                 where f.company.comId = :comId
                   and f.writer.empId = :writerId
                   and f.docfoStat = :stat
                   and lower(f.docfoName) like lower(concat('%', :keyword, '%'))
                 order by f.docfoNo desc
            """)
    Page<DocumentForm> searchMyByStat(
            @Param("comId") String comId,
            @Param("writerId") String writerId,
            @Param("stat") DocumentFormStats stat,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    delete from DocumentForm f
    where f.docfoNo = :docfoNo
      and f.company.comId = :comId
      and f.writer.empId = :writerId
      and f.docfoStat = :stat
    """)
    int deleteMyTempById(
            @Param("docfoNo") Long docfoNo,
            @Param("comId") String comId,
            @Param("writerId") String writerId,
            @Param("stat") DocumentFormStats stat
    );
}