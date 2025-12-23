package com.multi.mlpenterpriseapprovalsystem.document_form.form.repository;

import com.multi.mlpenterpriseapprovalsystem.document_form.form.domain.*;
import org.springframework.data.jpa.repository.*;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : DocumentFormCategoryRepository
 * @since : 2025-12-22 월요일
 */
public interface DocumentFormCategoryRepository extends JpaRepository<DocumentFormCategory, Long> {
    List<DocumentFormCategory> findByDocumentForm_DocfoNo(Long docfoNo);
}