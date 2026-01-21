package com.multi.mlpenterpriseapprovalsystem.documentform.form.repository;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.util.*;

/**
 * 문서양식 카테고리 관리 repository
 *
 * @author : 정종원
 * @filename : DocumentFormCategoryRepository
 * @since : 2025-12-22 월요일
 */

public interface DocumentFormCategoryRepository extends JpaRepository<DocumentFormCategory, Long> {
    void deleteByDocumentForm(DocumentForm documentForm);
    List<DocumentFormCategory> findByDocumentForm_DocfoNoOrderByDocfoCatNoAsc(Long docfoNo);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DocumentFormCategory c where c.documentForm.docfoNo = :docfoNo")
    int deleteByDocfoNo(@Param("docfoNo") Long docfoNo);

    // 문서 양식 내 카테고리 이름만 중복없이 조회
    @Query("SELECT DISTINCT dfc.name FROM DocumentFormCategory dfc WHERE dfc.company.comId = :comId")
    List<String> findDistinctNamesByComId(@Param("comId") String comId);

    List<DocumentFormCategory> findAllByDocumentForm_DocfoNo(Long docfoNo);
}