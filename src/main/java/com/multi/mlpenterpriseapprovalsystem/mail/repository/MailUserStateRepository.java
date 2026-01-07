package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.MailUserState;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MailUserStateRepository extends JpaRepository<MailUserState, Long> {

    // mailNo 기반(메인)
    Optional<MailUserState> findByMail_MailNoAndUser_EmpId(Long mailNo, String userEmpId);

    //  recipients (SENDER 화면용)

    // (mailNo 기반) - 서비스 toListDto에서 사용
    @Query("""
        select mus.user.empName
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientNamesByMailNo(@Param("mailNo") Long mailNo);

    // (mailNo 기반) - 서비스 detail에서 사용
    @Query("""
        select concat(mus.user.empName, '(', mus.user.empId, ')')
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientDisplayByMailNo(@Param("mailNo") Long mailNo);

    //  inbox/sent
    @Query(
            value = """
        select mus
        from MailUserState mus
        join fetch mus.mail m
        join fetch m.sender s
        where mus.user.empId = :userEmpId
          and mus.role = :role
          and mus.deletedAt is null
          and (:q is null or :q = '' or m.title like concat('%', :q, '%'))
        order by m.mailNo desc
    """,
            countQuery = """
        select count(mus)
        from MailUserState mus
        join mus.mail m
        where mus.user.empId = :userEmpId
          and mus.role = :role
          and mus.deletedAt is null
          and (:q is null or :q = '' or m.title like concat('%', :q, '%'))
    """
    )
    Page<MailUserState> findMailbox(
            @Param("userEmpId") String userEmpId,
            @Param("role") MailRole role,
            @Param("q") String q,
            Pageable pageable
    );

    // trash
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

    int countByUserAndIsReadFalse(Employee user);
}