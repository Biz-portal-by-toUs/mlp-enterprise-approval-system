package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 결재라인 테이블 관리 repository
 *
 * @author : 이지헌
 * @filename : ApprovalLineRepository
 * @since : 25. 12. 18. 목요일.
 */
public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {

    List<ApprovalLine> findByDocument_docNo(Long docNo);

    @Modifying
    void deleteByDocument_docNo(Long docNo);


    /**
     * 휴가 종료 시 대직자 결재라인 삭제 (2단계 방식)
     * Step 1: 삭제할 결재라인 ID 조회
     */
//    @Query("SELECT al.apprlNo FROM ApprovalLine al " +
//            "WHERE al.isDelegate = true " +
//            "AND al.approver = :delegate " +
//            "AND al.apprStat IN (:apprStats) " +
//            "AND al.document.docStat IN (:docStats) " +
//            "AND EXISTS (" +
//            "    SELECT 1 FROM ApprovalLine al2 " +
//            "    WHERE al2.document = al.document " +
//            "    AND al2.seq = al.seq " +
//            "    AND al2.isDelegate = false " +
//            "    AND al2.approver = :onLeave" +
//            ")")
//    List<Long> findDelegateApprovalLineIdsToDelete(
//            @Param("onLeave") Employee onLeave,
//            @Param("delegate") Employee delegate,
//            @Param("docStats") DocStat[] docStats,
//            @Param("apprStats") ApprStat[] apprStats
//    );

    @Query("SELECT al.apprlNo FROM ApprovalLine al " +
            "WHERE al.isDelegate = true " +
            "AND al.approver = :delegate " +
            "AND al.targetApprover = :onLeave " + // ◀ seq를 일일이 대조할 필요 없이 직접 관계 확인 가능
            "AND al.apprStat IN (:apprStats) " +
            "AND al.document.docStat IN (:docStats)")
    List<Long> findDelegateApprovalLineIdsToDelete(
            @Param("onLeave") Employee onLeave,
            @Param("delegate") Employee delegate,
            @Param("docStats") DocStat[] docStats,
            @Param("apprStats") ApprStat[] apprStats
    );

    /**
     * 결재라인 ID 목록으로 삭제 (Step 2)
     */
    @Modifying
    @Query("DELETE FROM ApprovalLine al WHERE al.apprlNo IN :apprlNos")
    void deleteByApprlNoIn(@Param("apprlNos") List<Long> apprlNos);

    /**
     * 문서상태가 US(임시저장), 결재중(AW)이고,
     * 휴가자의 결재상태가 I(결재중), W(결재대기중)인 문서에서
     * 휴가자의 결재라인 조회
     */
    @Query("SELECT al FROM ApprovalLine al " +
            /* "WHERE al.isDelegate = false " + */ //휴가자가 원결재자가 아닌 대직자일 수도 있음
            "WHERE al.approver = :onLeave " +
            "AND al.apprStat IN (:apprStats) " +
            "AND al.document.docStat IN (:docStats)")
    List<ApprovalLine> findApprovalLinesForVacation(
            @Param("onLeave") Employee onLeave,
            @Param("docStats") DocStat[] docStats,
            @Param("apprStats") ApprStat[] apprStats
    );

    //
//    boolean existsByDocumentAndSeqAndApproverAndIsDelegate(
//            Document document,
//            int seq,
//            Employee approver,
//            Boolean isDelegate
//    );


    boolean existsByDocumentAndSeqAndApproverAndIsDelegateAndTargetApprover(
            Document document,
            int seq,
            Employee approver,
            Boolean isDelegate,
            Employee targetApprover
    );

    // 내가 결재자이면서 결재 상태가 AWAITING인 문서 개수 카운트
    int countByApproverAndApprStat(Employee approver, ApprStat apprStat);
}
