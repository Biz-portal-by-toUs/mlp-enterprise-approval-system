package com.multi.mlpenterpriseapprovalsystem.mail.repository;

import com.multi.mlpenterpriseapprovalsystem.mail.domain.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.*;

import java.time.*;
import java.util.*;

/**
 * 메일 상태 관리 repository
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

    // 임시저장(Draft) 전용
    // 규칙: savedAt != null 이면 임시저장

    // 내 임시저장 목록 (검색 없음)
    @Query(
            value = """
            select m
            from Mail m
            where m.sender.empId = :senderEmpId
              and m.savedAt is not null
            order by m.savedAt desc
        """,
            countQuery = """
            select count(m)
            from Mail m
            where m.sender.empId = :senderEmpId
              and m.savedAt is not null
        """
    )
    Page<Mail> findDrafts(@Param("senderEmpId") String senderEmpId, Pageable pageable);

    // 내 임시저장 목록 (제목 검색 포함)
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

    // 특정 임시저장 단건 조회 (내 것만 + sender fetch)
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

    // 임시저장인지 체크
    boolean existsByMailIdAndSender_EmpIdAndSavedAtIsNotNull(String mailId, String senderEmpId);

    // 임시저장 삭제 (내 것만)
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

    // 임시저장 "정리"용: 일정 기간 지난 초안 삭제 (옵션)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from Mail m
            where m.savedAt is not null
              and m.savedAt < :threshold
        """)
    int deleteOldDrafts(@Param("threshold") LocalDateTime threshold);

    // 내 임시저장 개수
    long countBySender_EmpIdAndSavedAtIsNotNull(String senderEmpId);

    // (선택) mailNo 기준 단건 조회
    Optional<Mail> findByMailNo(Long mailNo);

    // (선택) mailNo 기준 상세 + sender fetch
    @Query("""
        select m
        from Mail m
        join fetch m.sender
        where m.mailNo = :mailNo
    """)
    Optional<Mail> findDetailByMailNo(@Param("mailNo") Long mailNo);
}