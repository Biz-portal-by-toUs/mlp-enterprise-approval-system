package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.util.*;

public interface MailUserStateRepository extends JpaRepository<MailUserState, Long> {

    Optional<MailUserState> findByMail_MailNoAndUser_EmpId(Long mailNo, String userEmpId);

    @Query("""
        select mus.user.empName
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientNamesByMailNo(@Param("mailNo") Long mailNo);

    @Query("""
        select concat(mus.user.empName, '(', mus.user.empId, ')')
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientDisplayByMailNo(@Param("mailNo") Long mailNo);

    // inbox/sent 공용
    @Query(
            value = """
            select mus
            from MailUserState mus
            join fetch mus.mail m
            join fetch m.sender s
            where mus.user.empId = :userEmpId
              and mus.role = :role
              and mus.deletedAt is null
              and m.sender.empId <> mus.user.empId
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
              and m.sender.empId <> mus.user.empId
              and (:q is null or :q = '' or m.title like concat('%', :q, '%'))
        """
    )
    Page<MailUserState> findMailbox(
            @Param("userEmpId") String userEmpId,
            @Param("role") MailRole role,
            @Param("q") String q,
            Pageable pageable
    );

    // 내게쓴메일함 전용 (self mail만)
    @Query(
            value = """
            select mus
            from MailUserState mus
            join fetch mus.mail m
            join fetch m.sender s
            where mus.user.empId = :userEmpId
              and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
              and mus.deletedAt is null
              and m.sender.empId = mus.user.empId
              and (:q is null or :q = '' or m.title like concat('%', :q, '%'))
            order by m.mailNo desc
        """,
            countQuery = """
            select count(mus)
            from MailUserState mus
            join mus.mail m
            where mus.user.empId = :userEmpId
              and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
              and mus.deletedAt is null
              and m.sender.empId = mus.user.empId
              and (:q is null or :q = '' or m.title like concat('%', :q, '%'))
        """
    )
    Page<MailUserState> findSelfMailbox(
            @Param("userEmpId") String userEmpId,
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
    Page<MailUserState> findTrash(@Param("userEmpId") String userEmpId, Pageable pageable);

    int countByUserAndIsReadFalse(Employee user);
}