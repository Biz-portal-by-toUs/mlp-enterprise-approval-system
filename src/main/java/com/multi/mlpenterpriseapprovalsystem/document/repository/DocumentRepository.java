package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 문서 테이블 관리 repository
 *
 * @author : 이지헌
 * @filename : DocumentRepository
 * @since : 25. 12. 17. 수요일
 */

public interface DocumentRepository extends JpaRepository<Document, Long> {

    // 내 회사의 문서 중 내가 상신한 문서 조회
    // 작성자가 나인 문서 반환
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " +
            "WHERE d.company.comId = :comId " +
            "AND d.writer.empId = :empId " +
            "AND d.temp = false")
    Page<Document> getMySubmittedDocuments(@Param("comId") String comId,
                                           @Param("empId") String empId,
                                           Pageable pageable);

    // 내 회사의 문서 중 내가 상신한 문서 조회
    // 작성자가 나인 문서 반환(결재중, 최종승인, 반려)
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " +
            "WHERE d.company.comId = :comId " +
            "AND d.writer.empId = :empId " +
            "AND d.temp = false " +
            "ORDER BY CASE WHEN d.docStat = :docStat THEN 0 ELSE 1 END ASC, " +
            "d.createdAt DESC")
    Page<Document> getMySubmittedDocumentsByDocStat(@Param("comId") String comId,
                                           @Param("empId") String empId,
                                           @Param("docStat") DocStat docStat,
                                           Pageable pageable);

//======================================================================================================================

    // 내 회사의 문서 중 내가 결재할 문서 조회
    // 결재라인에 내가 있고 상태가 결재중, 결재대기중인 문서 조회(결재중이 먼저 오도록)
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " + // DISTINCT 제거
            "JOIN d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            "AND d.docStat = :docStatAW " +
            "AND al.approver.empId = :empId " +
            "AND al.apprStat IN (:apprStatI, " + ":apprStatW) " +
            "ORDER BY CASE WHEN al.apprStat = :apprStatI THEN 0 " +
            "              WHEN al.apprStat = :apprStatW THEN 1 " +
            "              ELSE 2 END ASC, d.createdAt DESC")
    Page<Document> getAwaitingMyApprovalDocuments(@Param("comId")String comId,
                                                  @Param("empId") String empId,
                                                  @Param("apprStatI")ApprStat apprStatI,
                                                  @Param("apprStatW")ApprStat apprStatW,
                                                  @Param("docStatAW")DocStat docStatAW,
                                                  Pageable pageable);

//======================================================================================================================

    // 내 회사의 문서 중 내가 결재한 문서 조회
    // 결재라인에 내가 있고 결재상태가 승인 or 반려인 문서 조회
    // 내가 결재한 문서 조회 - 내 결재일 기준 최신순 (기본)
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " +
            "JOIN d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            "AND al.approver.empId = :empId " +
            "AND al.apprStat IN (:apprStatA, :apprStatR) " +
            "ORDER BY al.endedAt DESC")
    Page<Document> getMyProcessedDocumentsLatest(@Param("comId") String comId,
                                           @Param("empId") String empId,
                                           @Param("apprStatA") ApprStat apprStatA,
                                           @Param("apprStatR") ApprStat apprStatR,
                                           Pageable pageable);

    // 내가 결재한 문서 조회 - 내 결재일 기준 오래된순
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " +
            "JOIN d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            "AND al.approver.empId = :empId " +
            "AND al.apprStat IN (:apprStatA, :apprStatR) " +
            "ORDER BY al.endedAt ASC")
    Page<Document> getMyProcessedDocumentsOldest(@Param("comId") String comId,
                                                 @Param("empId") String empId,
                                                 @Param("apprStatA") ApprStat apprStatA,
                                                 @Param("apprStatR") ApprStat apprStatR,
                                                 Pageable pageable);

    // 내가 결재한 문서 조회 - 특정 내 결재상태 우선 + 내 결재일 기준 최신순
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " +
            "JOIN d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            "AND al.approver.empId = :empId " +
            "AND al.apprStat IN (:apprStatA, :apprStatR) " +
            "ORDER BY CASE WHEN al.apprStat = :myApprStat THEN 0 ELSE 1 END ASC, " +
            "al.endedAt DESC")
    Page<Document> getMyProcessedDocumentsByMyApprStat(@Param("comId") String comId,
                                                       @Param("empId") String empId,
                                                       @Param("apprStatA") ApprStat apprStatA,
                                                       @Param("apprStatR") ApprStat apprStatR,
                                                       @Param("myApprStat") ApprStat myApprStat,
                                                       Pageable pageable);

//======================================================================================================================

    // 내 회사의 최종승인문서 조회
    // DocStat이 FI인 문서 모두 조회(최근 결재순(sort=LATEST or sort=NULL), 오래된 결재순(sort=OLDEST))
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " +
            "WHERE d.company.comId = :comId " +
            "AND d.temp = false " +
            "AND d.docStat = :docStatFI")
    Page<Document> getFinalizedDocuments(@Param("comId")String comId,
                                         @Param("docStatFI")DocStat docStatFI,
                                         Pageable pageable);
}
