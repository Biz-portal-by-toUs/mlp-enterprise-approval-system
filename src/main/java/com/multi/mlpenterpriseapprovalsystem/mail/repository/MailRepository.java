package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.mail.domain.Mail;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MailRepository extends JpaRepository<Mail, Long> {

    // drafts 목록 (keyword optional)
    @Query(
            value = """
            select m
            from Mail m
            where m.sender.empId = :senderEmpId
              and m.savedAt is not null
              and (:q is null or :q = '' or m.title like concat('%', :q, '%'))
            order by m.savedAt desc
        """,
            countQuery = """
            select count(m)
            from Mail m
            where m.sender.empId = :senderEmpId
              and m.savedAt is not null
              and (:q is null or :q = '' or m.title like concat('%', :q, '%'))
        """
    )
    Page<Mail> findDrafts(
            @Param("senderEmpId") String senderEmpId,
            @Param("q") String q,
            Pageable pageable
    );

    // draft detail (내 것만)
    @Query("""
        select m
        from Mail m
        join fetch m.sender
        where m.mailId = :mailId
          and m.sender.empId = :senderEmpId
          and m.savedAt is not null
    """)
    Optional<Mail> findDraftDetail(
            @Param("mailId") String mailId,
            @Param("senderEmpId") String senderEmpId
    );

    // delete draft
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Mail m
        where m.mailId = :mailId
          and m.sender.empId = :senderEmpId
          and m.savedAt is not null
    """)
    int deleteDraft(
            @Param("mailId") String mailId,
            @Param("senderEmpId") String senderEmpId
    );

    // cleanup option
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from Mail m
        where m.savedAt is not null
          and m.savedAt < :threshold
    """)
    int deleteOldDrafts(@Param("threshold") LocalDateTime threshold);
}