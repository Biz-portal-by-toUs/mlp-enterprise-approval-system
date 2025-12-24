package com.multi.mlpenterpriseapprovalsystem.common.storage.repository;

import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentDomain;
import com.multi.mlpenterpriseapprovalsystem.common.storage.enums.AttachmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * attachment 관련 레포지토리
 *
 * @author : 권지영
 * @filename : AttachmentRepository
 * @since : 2025. 12. 24. 수요일
 */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    // ACTIVE 첨부(1~5 제한 체크용)
    long countByComIdAndDomainAndEntityIdAndStatus(
            String comId, AttachmentDomain domain, Long entityId, AttachmentStatus status
    );

    // 동시 업로드(complete 동시에 2번) 대비해서 같은 글의 첨부를 잠그고 읽기
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           select a from Attachment a
           where a.comId = :comId
             and a.domain = :domain
             and a.entityId = :entityId
           """)
    List<Attachment> findAllByRefForUpdate(
            @Param("comId") String comId,
            @Param("domain") AttachmentDomain domain,
            @Param("entityId") Long entityId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Attachment a where a.attachmentId = :id")
    Optional<Attachment> findByIdForUpdate(@Param("id") Long id);


    List<Attachment> findByComIdAndDomainAndEntityIdAndStatusOrderByDisplayOrderAsc(
            String comId, AttachmentDomain domain, Long entityId, AttachmentStatus status
    );

    Optional<Attachment> findByAttachmentIdAndComIdAndStatus(
            Long attachmentId, String comId, AttachmentStatus status
    );


}