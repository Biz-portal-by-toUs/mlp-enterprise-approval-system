package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}
