package com.multi.mlpenterpriseapprovalsystem.documentform.form.repository;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.*;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.enums.DocumentFormStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

/**
 * 문서 양식 관리 repository
 *
 * @author : 정종원
 * @filename : DocumentFormRepository
 * @since : 2025-12-22 월요일
 */

public interface DocumentFormRepository extends JpaRepository<DocumentForm, Long> {

    // 단건 조회 (회사 스코프)
    Optional<DocumentForm> findByDocfoNoAndCompany_ComId(Long docfoNo, String comId);

    // 목록/검색 (상태 다건)
    Page<DocumentForm> findByDocfoStatInAndCompany_ComIdOrderByDocfoNoAsc(
            List<DocumentFormStats> stats,
            String comId,
            Pageable pageable
    );

    Page<DocumentForm> findByDocfoStatInAndCompany_ComIdAndDocfoNameContainingIgnoreCaseOrderByDocfoNoAsc(
            List<DocumentFormStats> stats,
            String comId,
            String docfoName,
            Pageable pageable
    );

    // 상태 변경 (승인/반려/삭제요청 등)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DocumentForm f
           set f.docfoStat = :stat,
               f.rejectReason = :reason
         where f.docfoNo = :docfoNo
    """)
    int updateStatusAndReason(
            @Param("docfoNo") Long docfoNo,
            @Param("stat") DocumentFormStats stat,
            @Param("reason") String reason
    );

    // 임시저장 전용 업데이트
    @Modifying(clearAutomatically = true, flushAutomatically = true)
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

    // 내 임시저장 목록/검색
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

    // 내 임시저장 완전삭제
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

    // 수정본(편집본) 1개 제한 체크
    boolean existsByCompany_ComIdAndOriginDocfoNoAndDocfoStatIn(
            String comId,
            Long originDocfoNo,
            List<DocumentFormStats> stats
    );

    // 수정본 승인 스왑용
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DocumentForm f
           set f.docfoStat = :toStat,
               f.rejectReason = null
         where f.docfoNo = :originDocfoNo
           and f.company.comId = :comId
           and f.docfoStat = :fromStat
    """)
    int updateStatusForOrigin(
            @Param("originDocfoNo") Long originDocfoNo,
            @Param("comId") String comId,
            @Param("fromStat") DocumentFormStats fromStat,
            @Param("toStat") DocumentFormStats toStat
    );

    /* @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DocumentForm f
           set f.docfoStat = :toStat,
               f.rejectReason = null
         where f.docfoNo = :docfoNo
           and f.company.comId = :comId
           and f.docfoStat in :fromStats
    """)
    int approveEditedDraft(
            @Param("docfoNo") Long docfoNo,
            @Param("comId") String comId,
            @Param("fromStats") List<DocumentFormStats> fromStats,
            @Param("toStat") DocumentFormStats toStat
    );
    */
}