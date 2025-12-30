package com.multi.mlpenterpriseapprovalsystem.prov_document.repository;

import com.multi.mlpenterpriseapprovalsystem.prov_document.domain.ProvDocument;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사내 규정 repository
 *
 * @author : 김승기
 * @filename : ProvDocumentRepository
 * @since : 2025. 12. 29. 월요일
 */
public interface ProvDocumentRepository extends JpaRepository<ProvDocument, Long> {

}
