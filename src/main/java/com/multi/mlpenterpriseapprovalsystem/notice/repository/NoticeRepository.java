package com.multi.mlpenterpriseapprovalsystem.notice.repository;

import com.multi.mlpenterpriseapprovalsystem.notice.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : NoticeRepository
 * @since : 2025-12-16 화요일
 */

public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {

    List<Notice> findByCompany_ComId(String comId);
    Page<Notice> findByCompany_ComIdAndIsDeletedFalse(String comId, Pageable pageable);

    @Query("select p from  Notice  p where p.company.comId =:comId and p.isPopup = true and p.startedAt <= current date and p.endedAt >= current date ")
    List<Notice> findByCompanyAndStatus(@Param("comId") String comId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notice n set n.rating = coalesce(n.rating, 0) + 1 where n.noticeNo = :noticeNo")
    int incrementRating(@Param("noticeNo") Long noticeNo);



}
