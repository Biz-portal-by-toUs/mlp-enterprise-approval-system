package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 결재라인 테이블 관리 repository
 *
 * @author : 이지헌
 * @filename : ApprovalLineRepository
 * @since : 25. 12. 18. 목요일
 */
public interface ApprovalLineRepository extends JpaRepository<ApprovalLine, Long> {

    Optional<List<ApprovalLine>> findByDocument_docId(Long doctId);

}
