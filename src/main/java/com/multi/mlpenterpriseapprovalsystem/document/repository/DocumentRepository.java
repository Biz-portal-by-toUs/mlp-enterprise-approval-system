package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 문서 테이블 관리 repository
 *
 * @author : 이지헌
 * @filename : DocumentRepository
 * @since : 25. 12. 17. 수요일
 */

public interface DocumentRepository extends JpaRepository<Document, Long> {


    @Query("SELECT DISTINCT d FROM Document d " +
            "JOIN FETCH d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            "AND d.temp = false")
    List<Document> findAllWithApprovalLinesByComId(@Param("comId") String comId);

    // 내가 상신한 문서 조회
    @Query("SELECT d FROM Document d " +
            "WHERE d.company.comId = :comId " +
            "AND d.writer.empId = :empId " +
            "AND d.temp = false")
    Page<Document> getMySubmittedDocuments(@Param("comId") String comId, @Param("empId") String empId, Pageable pageable);

    // 내가 결재할 문서 조회
    // 결재라인에 내가 있고 상태가 I인 문서들을 모두 반환해야함
    @EntityGraph(attributePaths = {"writer", "documentForm"})
    @Query("SELECT d FROM Document d " + // DISTINCT 제거
            "JOIN d.approvalLines al " +
            "WHERE d.company.comId = :comId " +
            "AND d.docStat = com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat.AW " +
            "AND al.approver.empId = :empId " +
            "AND al.apprStat IN (com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat.I, " +
            "com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat.W) " +
            "ORDER BY CASE WHEN al.apprStat = com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat.I THEN 0 " +
            "              WHEN al.apprStat = com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat.W THEN 1 " +
            "              ELSE 2 END ASC, d.createdAt DESC")
    Page<Document> getDocumentsAwaitingMyApproval(@Param("comId") String comId, @Param("empId") String empId, Pageable pageable);
}
