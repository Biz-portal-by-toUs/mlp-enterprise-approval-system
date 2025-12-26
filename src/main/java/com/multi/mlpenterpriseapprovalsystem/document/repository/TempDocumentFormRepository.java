package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.DocumentForm;
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
}
