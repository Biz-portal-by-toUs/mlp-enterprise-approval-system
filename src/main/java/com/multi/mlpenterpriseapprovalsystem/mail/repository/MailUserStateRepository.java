package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import com.multi.mlpenterpriseapprovalsystem.mail.enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.time.*;
import java.util.*;

public interface MailUserStateRepository extends JpaRepository<MailUserState, Long> {

    Optional<MailUserState> findByMail_MailNoAndUser_EmpId(Long mailNo, String userEmpId);

    @Query("""
        select mus.user.empName
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
        order by mus.user.empName asc
    """)
    List<String> findRecipientNamesByMailNo(@Param("mailNo") Long mailNo);

    @Query("""
        select concat(mus.user.empName, '(', mus.user.empId, ')')
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
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
          and (:keyword is null or :keyword = '' or m.title like concat('%', :keyword, '%'))
          and (:fromDt is null or m.createdAt >= :fromDt)
          and (:toDt   is null or m.createdAt <= :toDt)
          and exists (
              select 1
              from MailUserState x
              where x.mail.mailNo = m.mailNo
                and x.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.SENDER
          )
        order by m.mailNo desc
    """,
            countQuery = """
        select count(mus)
        from MailUserState mus
        join mus.mail m
        where mus.user.empId = :userEmpId
          and mus.role = :role
          and mus.deletedAt is null
          and (:keyword is null or :keyword = '' or m.title like concat('%', :keyword, '%'))
          and (:fromDt is null or m.createdAt >= :fromDt)
          and (:toDt   is null or m.createdAt <= :toDt)
          and exists (
              select 1
              from MailUserState x
              where x.mail.mailNo = m.mailNo
                and x.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.SENDER
          )
    """
    )
    Page<MailUserState> findMailbox(
            @Param("userEmpId") String userEmpId,
            @Param("role") MailRole role,
            @Param("keyword") String keyword,
            @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt,
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
          and (:keyword is null or :keyword = '' or m.title like concat('%', :keyword, '%'))
          and (:fromDt is null or m.createdAt >= :fromDt)
          and (:toDt   is null or m.createdAt <= :toDt)
          and not exists (
              select 1
              from MailUserState x
              where x.mail.mailNo = m.mailNo
                and x.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.SENDER
          )
        order by m.mailNo desc
    """,
            countQuery = """
        select count(mus)
        from MailUserState mus
        join mus.mail m
        where mus.user.empId = :userEmpId
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
          and (:keyword is null or :keyword = '' or m.title like concat('%', :keyword, '%'))
          and (:fromDt is null or m.createdAt >= :fromDt)
          and (:toDt   is null or m.createdAt <= :toDt)
          and not exists (
              select 1
              from MailUserState x
              where x.mail.mailNo = m.mailNo
                and x.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.SENDER
          )
    """
    )
    Page<MailUserState> findSelfMailbox(
            @Param("userEmpId") String userEmpId,
            @Param("keyword") String keyword,
            @Param("fromDt") LocalDateTime fromDt,
            @Param("toDt") LocalDateTime toDt,
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