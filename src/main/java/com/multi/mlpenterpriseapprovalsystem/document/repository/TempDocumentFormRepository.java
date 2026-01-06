package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentForm;
import com.multi.mlpenterpriseapprovalsystem.documentform.form.enums.DocumentFormStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : DocumentFormRepository
 * @since : 25. 12. 22. 월요일
 */
public interface TempDocumentFormRepository extends JpaRepository<DocumentForm, Long> {
    List<DocumentForm> findAllByCompany_comId(String comId);

    // 회사의 승인된 문서 양식만 조회
    List<DocumentForm> findAllByCompany_ComIdAndDocfoStat(String comId, DocumentFormStats docfoStat);
}
