package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.mail.domain.MailUserState;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MailUserStateRepository extends JpaRepository<MailUserState, Long> {

    // 특정 메일에서 특정 유저의 상태 row 단건 조회
    Optional<MailUserState> findByMail_MailIdAndUser_EmpId(String mailId, String userEmpId);

    @Query("""
        select mus
        from MailUserState mus
        where mus.mail.mailId = :mailId
          and mus.user.empId = :userEmpId
    """)
    Optional<MailUserState> findState(
            @Param("mailId") String mailId,
            @Param("userEmpId") String userEmpId
    );

    // 특정 메일 전체 참여자 상태들 조회
    List<MailUserState> findAllByMail_MailId(String mailId);

    // 보낸 메일함에서 "수신인 이름들" 뽑기 (RECIPIENT만)
    @Query("""
        select mus.user.empName
        from MailUserState mus
        where mus.mail.mailId = :mailId
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientNamesByMailId(@Param("mailId") String mailId);

    // 받은 메일함 (Inbox)
    @Query(
            value = """
                select mus
                from MailUserState mus
                join fetch mus.mail m
                join fetch m.sender s
                where mus.user.empId = :userEmpId
                  and mus.role = :role
                  and mus.deletedAt is null
                order by m.createdAt desc
            """,
            countQuery = """
                select count(mus)
                from MailUserState mus
                where mus.user.empId = :userEmpId
                  and mus.role = :role
                  and mus.deletedAt is null
            """
    )
    Page<MailUserState> findInbox(
            @Param("userEmpId") String userEmpId,
            @Param("role") MailRole role,
            Pageable pageable
    );

    // 보낸 메일함 (Sent)
    @Query(
            value = """
                select mus
                from MailUserState mus
                join fetch mus.mail m
                join fetch m.sender s
                where mus.user.empId = :userEmpId
                  and mus.role = :role
                  and mus.deletedAt is null
                order by m.createdAt desc
            """,
            countQuery = """
                select count(mus)
                from MailUserState mus
                where mus.user.empId = :userEmpId
                  and mus.role = :role
                  and mus.deletedAt is null
            """
    )
    Page<MailUserState> findSent(
            @Param("userEmpId") String userEmpId,
            @Param("role") MailRole role,
            Pageable pageable
    );

    // 휴지통 (Trash) - mail + sender fetch
    @Query(
            value = """
                select mus
                from MailUserState mus
                join fetch mus.mail m
                join fetch m.sender s
                where mus.user.empId = :userEmpId
                  and mus.deletedAt is not null
                order by mus.deletedAt desc
            """,
            countQuery = """
                select count(mus)
                from MailUserState mus
                where mus.user.empId = :userEmpId
                  and mus.deletedAt is not null
            """
    )
    Page<MailUserState> findTrash(
            @Param("userEmpId") String userEmpId,
            Pageable pageable
    );
}