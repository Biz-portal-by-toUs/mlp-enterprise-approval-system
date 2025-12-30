package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : MailRepository
 * @since : 2025-12-30 화요일
 */
public interface MailRepository extends JpaRepository<Mail, Long> {
    // 메일 단건 조회 (mailId 기준)
    Optional<Mail> findByMailId(String mailId);

    // 메일 단건 조회 + 발신자까지 fetch (상세조회 화면에서 자주 씀)
    @Query("""
                select m
                from Mail m
                join fetch m.sender
                where m.mailId = :mailId
            """)
    Optional<Mail> findDetailByMailId(@Param("mailId") String mailId);

    // 보낸 메일함 조회 (sender 기준)
    @Query(value = """
                select m
                from Mail m
                where m.sender.empId = :senderEmpId
                  and exists (
                      select 1
                      from MailUserState mus
                      where mus.mail = m
                        and mus.user.empId = :senderEmpId
                  )
                order by m.createdAt desc
            """,
            countQuery = """
                        select count(m)
                        from Mail m
                        where m.sender.empId = :senderEmpId
                          and exists (
                              select 1
                              from MailUserState mus
                              where mus.mail = m
                                and mus.user.empId = :senderEmpId
                          )
                    """)
    Page<Mail> findSentMails(@Param("senderEmpId") String senderEmpId, Pageable pageable);

    // 받은 메일함 조회 (recipient 기준)
    @Query(value = """
                select distinct m
                from MailUserState mus
                join mus.mail m
                join fetch m.sender
                where mus.user.empId = :receiverEmpId
                  and mus.deletedAt is null
                order by m.createdAt desc
            """,
            countQuery = """
                        select count(distinct m)
                        from MailUserState mus
                        join mus.mail m
                        where mus.user.empId = :receiverEmpId
                          and mus.deletedAt is null
                    """)
    Page<Mail> findInboxMails(@Param("receiverEmpId") String receiverEmpId, Pageable pageable);
}
