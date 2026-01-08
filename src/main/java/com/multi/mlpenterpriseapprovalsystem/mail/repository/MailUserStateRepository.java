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

    // =========================
    // ===== mailId 기반(호환용) =====
    // =========================

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

    // =========================
    // ✅ mailNo 기반(메인) =====
    // =========================

    Optional<MailUserState> findByMail_MailNoAndUser_EmpId(Long mailNo, String userEmpId);

    @Query("""
        select mus
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.user.empId = :userEmpId
    """)
    Optional<MailUserState> findStateByMailNo(
            @Param("mailNo") Long mailNo,
            @Param("userEmpId") String userEmpId
    );

    // ✅ (서비스 purge/restore/read 등에서 “mailNo + user”로 찾을 때 필요)
    // 이미 findByMail_MailNoAndUser_EmpId가 있어서 사실상 대체 가능하지만,
    // findStateByMailNo 스타일로 통일하고 싶으면 아래처럼 추가해도 됨(선택)
    // Optional<MailUserState> findByMail_MailNoAndUser_EmpId(Long mailNo, String userEmpId);

    // =========================
    // ===== recipients (SENDER 화면용) =====
    // =========================

    // (mailId 기반)
    @Query("""
        select mus.user.empName
        from MailUserState mus
        where mus.mail.mailId = :mailId
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientNamesByMailId(@Param("mailId") String mailId);

    // ✅ (mailNo 기반) - 서비스 toListDto에서 사용
    @Query("""
        select mus.user.empName
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientNamesByMailNo(@Param("mailNo") Long mailNo);

    // (mailId 기반)
    @Query("""
        select concat(mus.user.empName, '(', mus.user.empId, ')')
        from MailUserState mus
        where mus.mail.mailId = :mailId
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientDisplayByMailId(@Param("mailId") String mailId);

    // ✅ (mailNo 기반) - 서비스 detail에서 사용
    @Query("""
        select concat(mus.user.empName, '(', mus.user.empId, ')')
        from MailUserState mus
        where mus.mail.mailNo = :mailNo
          and mus.role = com.multi.mlpenterpriseapprovalsystem.mail.enums.MailRole.RECIPIENT
          and mus.deletedAt is null
        order by mus.user.empName asc
    """)
    List<String> findRecipientDisplayByMailNo(@Param("mailNo") Long mailNo);

    // =========================
    // ===== inbox/sent/trash =====
    // =========================

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
            order by coalesce(mus.isPrior, false) desc, m.createdAt desc
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
    Page<MailUserState> findInbox(
            @Param("userEmpId") String userEmpId,
            @Param("role") MailRole role,
            @Param("q") String q,
            Pageable pageable
    );

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
            order by coalesce(mus.isPrior, false) desc, m.createdAt desc
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
    Page<MailUserState> findSent(
            @Param("userEmpId") String userEmpId,
            @Param("role") MailRole role,
            @Param("q") String q,
            Pageable pageable
    );

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

    // =========================
    // ✅ 안 읽은 메일 개수
    // =========================

    int countByUserAndIsReadFalse(Employee user);
}