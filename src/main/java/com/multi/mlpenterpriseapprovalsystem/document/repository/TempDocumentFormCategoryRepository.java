package com.multi.mlpenterpriseapprovalsystem.document.repository;

import com.multi.mlpenterpriseapprovalsystem.documentform.form.domain.DocumentFormCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 임시 문서양식 내 카테고리 테이블 관리 레포지토리
 *
 * @author : 이지헌
 * @filename : DocumentFormCategoryRepository
 * @since : 25. 12. 22. 월요일
 */
public interface TempDocumentFormCategoryRepository extends JpaRepository<DocumentFormCategory, Long> {

    List<DocumentFormCategory> findAllByDocumentForm_DocfoNo(Long docfoNo);

    // 문서 양식 내 카테고리 이름만 중복없이 조회
    @Query("SELECT DISTINCT dfc.name FROM DocumentFormCategory dfc WHERE dfc.company.comId = :comId")
    List<String> findDistinctNamesByComId(@Param("comId") String comId);
}
