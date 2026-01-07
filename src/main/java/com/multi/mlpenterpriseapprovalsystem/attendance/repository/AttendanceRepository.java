package com.multi.mlpenterpriseapprovalsystem.attendance.repository;

import com.multi.mlpenterpriseapprovalsystem.attendance.domain.Attendance;
import com.multi.mlpenterpriseapprovalsystem.attendance.enums.AtteType;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 근태테이블 관리 레포지토리
 *
 * @author : 이지헌
 * @filename : AttendanceRepository
 * @since : 25. 12. 29. 월요일
 */
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 전체 조회 (삭제 제외)
    @EntityGraph(attributePaths = {"employee", "employee.department", "document", "delegate"})
    Page<Attendance> findByCompany_ComIdAndIsDeletedFalseOrderByStartAtDesc(String comId, Pageable pageable);

    // 내 근태 조회 (삭제 제외)
    List<Attendance> findByEmployee_EmpIdAndCompany_ComIdAndTypeAndIsDeletedFalseOrderByStartAtDesc(
            String empId, String comId, AtteType type);

    @Query("SELECT a FROM Attendance a " +
            "WHERE a.isDeleted = false " +
            "AND (FUNCTION('DATE', a.startAt) = :today " +
            "OR FUNCTION('DATE', a.endAt) = :yesterday)")
    List<Attendance> findByStartAtIsTodayOrEndAtIsYesterdayAndIsDeletedFalse(
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday
    );

    // 중복 체크 (삭제된 데이터는 무시해야 함)
    @Query("SELECT COUNT(a) > 0 FROM Attendance a " +
            "WHERE a.isDeleted = false AND a.employee = :employee " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    boolean existsByEmployeeAndDateOverlapAndIsDeletedFalse(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    // 대직자 선택 시 휴가 기간 중복 체크 (삭제된 데이터는 무시해야 함)
    @Query("SELECT COUNT(a) > 0 FROM Attendance a " +
            "WHERE a.isDeleted = false AND a.employee = :employee " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt AND a.type = :type")
    boolean existsByEmployeeAndDateOverlapAndTypeIsVAndIsDeletedFalse(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("type") AtteType type
    );


    // 수정 시 중복 체크 (본인 제외 + 삭제 제외)
    @Query("SELECT COUNT(a) > 0 FROM Attendance a " +
            "WHERE a.isDeleted = false AND a.employee = :employee AND a.atteNo != :excludeAtteNo " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    boolean existsByEmployeeAndDateOverlapExcludeSelfAndIsDeletedFalse(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeAtteNo") Long excludeAtteNo);

    /**
     * 수정 가능: 아직 끝나지 않은 특정 타입(V/B)의 근태 조회
     * 종료일(endAt)이 기준 시간(오늘) 이후인 데이터
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.employee.empId = :empId " +
            "AND a.company.comId = :comId " +
            "AND a.type = :type " +
            "AND a.isDeleted = false " +
            "AND a.endAt >= :today " +
            "ORDER BY a.startAt ASC")
    List<Attendance> findModifiableAttendances(
            @Param("comId") String comId,
            @Param("empId") String empId,
            @Param("today") LocalDateTime today,
            @Param("type") AtteType type);

    /**
     * 취소 가능: 아직 시작하지 않은 특정 타입(V/B)의 근태 조회
     * 시작일(startAt)이 기준 시간(오늘) 이후인 데이터
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.employee.empId = :empId " +
            "AND a.company.comId = :comId " +
            "AND a.type = :type " +
            "AND a.isDeleted = false " +
            "AND a.startAt >= :today " +
            "ORDER BY a.startAt ASC")
    List<Attendance> findCancelableAttendances(
            @Param("comId") String comId,
            @Param("empId") String empId,
            @Param("today") LocalDateTime today,
            @Param("type") AtteType type);

    /**
     * 특정 사원이 '다른 사람의 대직자'로서 활동해야 하는 기간이 있는지 확인
     * (나를 대직자로 지정한 다른 사람의 근태 기록 중, 기간이 겹치는 것이 있는지 체크)
     */
    @Query("""
    SELECT COUNT(a) > 0 FROM Attendance a 
    WHERE a.isDeleted = false 
      AND a.delegate = :me 
      AND a.startAt <= :endAt 
      AND a.endAt >= :startAt
    """)
    boolean existsByDelegateAndDateOverlap(
            @Param("me") Employee me,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );


    List<Attendance> findAllByDelegateAndIsDeletedFalse(Employee delegate);


    /**
     * 1. 신규 신청 시: 본인의 기존 근태 중복 목록 조회
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.isDeleted = false AND a.employee = :employee " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    List<Attendance> findAllByEmployeeAndDateOverlap(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    /**
     * 2. 수정 시: 본인 제외 및 기존 근태 중복 목록 조회
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.isDeleted = false AND a.employee = :employee " +
            "AND a.atteNo != :excludeAtteNo " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    List<Attendance> findAllByEmployeeAndDateOverlapExcludeSelf(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeAtteNo") Long excludeAtteNo);


}
