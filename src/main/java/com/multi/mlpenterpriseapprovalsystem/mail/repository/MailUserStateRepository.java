package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : MailUserStateRepository
 * @since : 2025-12-30 화요일
 */
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

    // role 고정(받은메일함 전용) 메서드
    @Query(
            value = """
                        select mus
                        from MailUserState mus
                        join fetch mus.mail m
                        join fetch m.sender s
                        where mus.user.empId = :userEmpId
                          and mus.deletedAt is null
                          and mus.role in :roles
                        order by m.createdAt desc
                    """,
            countQuery = """
                        select count(mus)
                        from MailUserState mus
                        where mus.user.empId = :userEmpId
                          and mus.deletedAt is null
                          and mus.role in :roles
                    """
    )
    Page<MailUserState> findInboxByRoles(
            @Param("userEmpId") String userEmpId,
            @Param("roles") List<MailRole> roles,
            Pageable pageable
    );

    // 보낸 메일함 (Sent)
    @Query(
            value = """
                        select mus
                        from MailUserState mus
                        join fetch mus.mail m
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

    // 휴지통 (Trash)
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
    Page<MailUserState> findTrash(@Param("userEmpId") String userEmpId, Pageable pageable);

    @Query(value = "select database()", nativeQuery = true)
    String currentDatabase();
}
