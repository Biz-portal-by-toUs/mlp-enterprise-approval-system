package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 문서 테이블 관리 repository
 *
 * @author : 이지헌
 * @filename : DocumentRepository
 * @since : 25. 12. 17. 수요일
 */

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // 내 회사의 문서 중 내가 임시저장한 문서 조회
    @EntityGraph(attributePaths = {"writer", "writer.department", "documentForm", "documentFormCategory"})
    @Query("SELECT d FROM Document d " +
            "WHERE d.company.comId = :comId " +
            "AND d.writer.empId = :myEmpId " + // 기안자가 본인인 문서
            "AND d.temp = true " +            // 임시저장만
            "AND d.docStat = :docStat " +
            "ORDER BY d.updatedAt DESC")    // 문서 상태 필터링
    Page<Document> searchMyUnSubmittedDocuments(@Param("comId") String comId,
                                                @Param("myEmpId") String myEmpId,
                                                @Param("docStat") DocStat docStatFilter1,
                                                Pageable pageable);

//======================================================================================================================

    // 내 회사의 문서 중 내가 상신한 문서 조회
    @EntityGraph(attributePaths = {"writer", "writer.department", "documentForm", "documentFormCategory"})
    @Query("SELECT d FROM Document d " +
            "WHERE d.company.comId = :comId " +
            "AND d.writer.empId = :myEmpId " + // 기안자가 본인인 문서
            "AND d.temp = false " +            // 임시저장 제외
            "AND d.docStat IN :docStats " +    // 문서 상태 필터링
            // 상세 검색 조건
            "AND (:docfoCatName IS NULL OR d.documentFormCategory.name = :docfoCatName) " +
            "AND (:depName IS NULL OR d.writer.department.depName = :depName) " +
            "AND (:docId IS NULL OR d.docId LIKE %:docId%) " +
            "AND (:title IS NULL OR d.title LIKE %:title%) " +
            "ORDER BY " +
            "CASE WHEN :sort = 'SUBMIT_LATEST' OR :sort IS NULL THEN d.submittedAt END DESC, " +
            "CASE WHEN :sort = 'SUBMIT_OLDEST' THEN d.submittedAt END ASC")
    Page<Document> searchMySubmittedDocuments(@Param("comId") String comId,
                                              @Param("myEmpId") String myEmpId,
                                              @Param("docfoCatName") String docfoCatName,
                                              @Param("depName") String depName,
                                              @Param("docId") String docId,
                                              @Param("title") String title,
                                              @Param("sort") String sort,
                                              @Param("docStats") List<DocStat> docStats,
                                              Pageable pageable);

//======================================================================================================================

    // 내 회사의 문서 중 내가 결재할 문서 조회
    // 내 회사의 문서 중 내가 결재할 문서 조회 (대기함 검색)
    @EntityGraph(attributePaths = {"writer", "writer.department", "documentForm", "documentFormCategory"})
    @Query(value = "SELECT d FROM Document d " +
            "JOIN d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            "AND d.docStat = :docStat " +
            // 본인이 결재자이거나 본인이 대직자인 경우
            "AND (al.approver.empId = :myEmpId OR al.approver.delegate.empId = :myEmpId) " +
            "AND al.apprStat IN :apprStats " +
            // 상세 검색 조건
            "AND (:docfoCatName IS NULL OR d.documentFormCategory.name = :docfoCatName) " +
            "AND (:depName IS NULL OR d.writer.department.depName = :depName) " +
            //"AND (:docId IS NULL OR d.docId = :docId) " +
            "AND (:title IS NULL OR d.title LIKE %:title%) " +
            "AND (:writerId IS NULL OR d.writer.empId = :writerId) " +
            "AND (:writerName IS NULL OR d.writer.empName LIKE %:writerName%) " +
            "GROUP BY d " + // DISTINCT 대신 GROUP BY d 사용으로 정렬 오류 해결
            "ORDER BY " +
            // 1. 정렬 조건 미선택 시: 내 결재 순서(I)가 먼저 오도록 함 (집계 함수 사용)
            "CASE WHEN :sort IS NULL OR :sort = '' THEN MIN(CASE WHEN al.apprStat = 'I' THEN 0 ELSE 1 END) END ASC, " +
            // 2. 상신일 기준 정렬
            "CASE WHEN :sort = 'SUBMIT_LATEST' THEN d.submittedAt END DESC, " +
            "CASE WHEN :sort = 'SUBMIT_OLDEST' THEN d.submittedAt END ASC, " +
            // 기본값: 상신일 최신순
            "d.submittedAt DESC",
            countQuery = "SELECT COUNT(DISTINCT d) FROM Document d JOIN d.approvalLines al " +
                    "WHERE d.company.comId = :comId AND d.docStat = :docStat " +
                    "AND (al.approver.empId = :myEmpId OR al.approver.delegate.empId = :myEmpId) " +
                    "AND al.apprStat IN :apprStats")
    Page<Document> searchAwaitingMyApprovalDocuments(@Param("comId") String comId,
                                                     @Param("myEmpId") String myEmpId,
                                                     @Param("docfoCatName") String docfoCatName,
                                                     @Param("depName") String depName,
                                                     @Param("docId") String docId,
                                                     @Param("title") String title,
                                                     @Param("writerId") String writerId,
                                                     @Param("writerName") String writerName,
                                                     @Param("sort") String sort,
                                                     @Param("docStat") DocStat docStat,
                                                     @Param("apprStats") List<ApprStat> apprStats,
                                                     Pageable pageable);

//======================================================================================================================

    // 내 회사의 문서 중 내가 결재한 문서 조회
    @EntityGraph(attributePaths = {"writer", "writer.department", "documentForm", "documentFormCategory"})
    @Query(value = "SELECT d FROM Document d " +
            "JOIN d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            // 결재자 본인이거나, 해당 결재자의 대직자(delegate)인 경우 조회
            "AND (al.approver.empId = :myEmpId OR al.approver.delegate.empId = :myEmpId) " +
            // 필터링: 문서 상태 (사용자 선택 또는 기본 AW, RJ, FI)
            "AND d.docStat IN :docStats " +
            // 필터링: 내 결재 상태 (사용자 선택 또는 기본 A, R)
            "AND al.apprStat IN :apprStats " +
            // 상세 검색 조건들
            "AND (:docfoCatName IS NULL OR d.documentFormCategory.name = :docfoCatName) " +
            "AND (:depName IS NULL OR d.writer.department.depName = :depName) " +
            "AND (:docId IS NULL OR d.docId LIKE %:docId%) " +
            "AND (:title IS NULL OR d.title LIKE %:title%) " +
            "AND (:writerId IS NULL OR d.writer.empId = :writerId) " +
            "AND (:writerName IS NULL OR d.writer.empName LIKE %:writerName%) " +
            "GROUP BY d " + // DISTINCT와 ORDER BY 충돌 해결을 위해 GROUP BY 사용
            "ORDER BY " +
            "CASE WHEN :sort = 'APPR_LATEST' THEN MAX(al.endedAt) END DESC, " + // 집계 함수로 정렬 모호성 제거
            "CASE WHEN :sort = 'APPR_OLDEST' THEN MIN(al.endedAt) END ASC, " +
            "CASE WHEN :sort = 'SUBMIT_LATEST' THEN d.submittedAt END DESC, " +
            "CASE WHEN :sort = 'SUBMIT_OLDEST' THEN d.submittedAt END ASC",
            countQuery = "SELECT COUNT(DISTINCT d) FROM Document d JOIN d.approvalLines al " +
                    "WHERE d.company.comId = :comId " +
                    "AND (al.approver.empId = :myEmpId OR al.approver.delegate.empId = :myEmpId) " +
                    "AND d.docStat IN :docStats AND al.apprStat IN :apprStats")
    Page<Document> searchMyProcessedDocuments(@Param("comId") String comId,
                                              @Param("myEmpId") String myEmpId,
                                              @Param("docfoCatName") String docfoCatName,
                                              @Param("depName") String depName,
                                              @Param("docId") String docId,
                                              @Param("title") String title,
                                              @Param("writerId") String writerId,
                                              @Param("writerName") String writerName,
                                              @Param("sort") String sort,
                                              @Param("docStats") List<DocStat> docStats,
                                              @Param("apprStats") List<ApprStat> apprStats,
                                              Pageable pageable);

//======================================================================================================================

    // 내 회사의 최종승인문서 조회
    @EntityGraph(attributePaths = {"writer", "writer.department", "documentForm", "documentFormCategory"})
    @Query("SELECT d FROM Document d " +
            "WHERE d.company.comId = :comId " +
            "AND d.docStat = :docStatFilter1 " +
            "AND (:docfoCatName IS NULL OR d.documentFormCategory.name = :docfoCatName) " +
            "AND (:depName IS NULL OR d.writer.department.depName = :depName) " +
            "AND (:docId IS NULL OR d.docId LIKE %:docId%) " +
            "AND (:title IS NULL OR d.title LIKE %:title%) " +
            "AND (:writerId IS NULL OR d.writer.empId = :writerId) " +
            "AND (:writerName IS NULL OR d.writer.empName LIKE %:writerName%) " +
            "ORDER BY " +
            // 1. 최종승인일(updatedAt) 기준 정렬
            "CASE WHEN :sort = 'FINALIZED_LATEST' OR :sort IS NULL THEN d.updatedAt END DESC, " +
            "CASE WHEN :sort = 'FINALIZED_OLDEST' THEN d.updatedAt END ASC, " +
            // 2. 상신일(submittedAt) 기준 정렬
            "CASE WHEN :sort = 'SUBMIT_LATEST' THEN d.submittedAt END DESC, " +
            "CASE WHEN :sort = 'SUBMIT_OLDEST' THEN d.submittedAt END ASC")
    Page<Document> searchFinalizedDocuments(@Param("comId") String comId,
                                            @Param("docfoCatName") String docfoCatName,
                                            @Param("depName") String depName,
                                            @Param("docId") String docId,
                                            @Param("title") String title,
                                            @Param("writerId") String writerId,
                                            @Param("writerName") String writerName,
                                            @Param("sort") String sort,
                                            Pageable pageable,
                                            @Param("docStatFilter1") DocStat docStatFilter1);

//======================================================================================================================

    // 문서코드로 문서 상세조회

    // 임시저장한 문서 검증: 작성자가 나이고 문서상태가 US(상신전)인 경우
    @Query("""
        SELECT d FROM Document d 
        WHERE d.company.comId = :comId 
          AND d.docNo = :docNo 
          AND d.writer.empId = :myEmpId 
          AND d.docStat = 'US'
    """)
    Optional<Document> findUnSubmittedDoc(@Param("comId") String comId, @Param("docNo") Long docNo, @Param("myEmpId") String myEmpId);


    // 상신한 문서 검증: 작성자가 나(myEmpId)이고 문서 상태가 AW(결재중), FI(최종승인), RJ(반려)인 경우
    @Query("""
        SELECT d FROM Document d 
        WHERE d.company.comId = :comId 
          AND d.docNo = :docNo 
          AND d.writer.empId = :myEmpId 
          AND d.docStat IN ('AW', 'FI', 'RJ')
    """)
    Optional<Document> findSubmittedDoc(@Param("comId") String comId,
                                        @Param("docNo") Long docNo,
                                        @Param("myEmpId") String myEmpId);

    // 결재할 문서 검증: 결재라인에 내가 있고, 내 결재 상태가 I(진행) 또는 W(대기)이며 문서가 AW 상태인 경우
    @Query("""
        SELECT DISTINCT d FROM Document d 
        JOIN d.approvalLines al 
        WHERE d.company.comId = :comId 
          AND d.docNo = :docNo 
          AND al.approver.empId = :myEmpId 
          AND d.docStat = 'AW' 
          AND al.apprStat IN ('I', 'W')
    """)
    Optional<Document> findAwaitingDoc(@Param("comId") String comId,
                                       @Param("docNo") Long docNo,
                                       @Param("myEmpId") String myEmpId);

    // 결재한 문서 검증: 결재라인에 내가 있고, 내 결재 상태가 A(승인) 또는 R(반려)인 경우
    @Query("""
        SELECT DISTINCT d FROM Document d 
        JOIN d.approvalLines al 
        WHERE d.company.comId = :comId 
          AND d.docNo = :docNo 
          AND al.approver.empId = :myEmpId 
          AND d.docStat IN ('AW', 'RJ', 'FI') 
          AND al.apprStat IN ('A', 'R')
    """)
    Optional<Document> findProcessedDoc(@Param("comId") String comId,
                                        @Param("docNo") Long docNo,
                                        @Param("myEmpId") String myEmpId);

    // 최종승인 문서 검증: 회사 식별자와 문서 번호가 일치하며 문서 상태가 FI(최종승인)인 경우
    @Query("""
        SELECT d FROM Document d 
        WHERE d.company.comId = :comId 
          AND d.docNo = :docNo 
          AND d.docStat = 'FI'
    """)
    Optional<Document> findFinalizedDoc(@Param("comId") String comId,
                                        @Param("docNo") Long docNo);

//======================================================================================================================

    /**
     * 해당 접두사로 시작하는 가장 마지막 문서코드 조회 (비관적 락)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d.docId FROM Document d WHERE d.docId LIKE :prefix% ORDER BY d.docId DESC LIMIT 1")
    String findLastDocIdByPrefixWithLock(@Param("prefix") String prefix);


//======================================================================================================================


}