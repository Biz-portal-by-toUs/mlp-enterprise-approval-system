package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.repository;

import com.multi.mlpenterpriseapprovalsystem.documentform.attachment.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.time.*;
import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : AttachBoxRepository
 * @since : 2026-01-10 토요일
 */
public interface AttachBoxRepository extends JpaRepository<AttachBox, Long> {

    // 목록 조회
    Page<AttachBox> findByCompany_ComIdAndCommittedTrueOrderByAttachNoDesc(String comId, Pageable pageable);

    // 단건 조회
    Optional<AttachBox> findByAttachNoAndCompany_ComId(Long attachNo, String comId);

    // 하드 삭제 (company scope 보장)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from AttachBox a
         where a.attachNo = :attachNo
           and a.company.comId = :comId
    """)
    int deleteByAttachNoAndComId(
            @Param("attachNo") Long attachNo,
            @Param("comId") String comId
    );

    // 30분 지난 미확정 데이터 정리
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    int deleteByCommittedFalseAndCreatedAtBefore(LocalDateTime threshold);
}