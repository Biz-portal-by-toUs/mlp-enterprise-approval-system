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

    // ✅ 중복 체크 (삭제된 데이터는 무시해야 함)
    @Query("SELECT COUNT(a) > 0 FROM Attendance a " +
            "WHERE a.isDeleted = false AND a.employee = :employee " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    boolean existsByEmployeeAndDateOverlapAndIsDeletedFalse(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    // ✅ 수정 시 중복 체크 (본인 제외 + 삭제 제외)
    @Query("SELECT COUNT(a) > 0 FROM Attendance a " +
            "WHERE a.isDeleted = false AND a.employee = :employee AND a.atteNo != :excludeAtteNo " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    boolean existsByEmployeeAndDateOverlapExcludeSelfAndIsDeletedFalse(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeAtteNo") Long excludeAtteNo);
}
