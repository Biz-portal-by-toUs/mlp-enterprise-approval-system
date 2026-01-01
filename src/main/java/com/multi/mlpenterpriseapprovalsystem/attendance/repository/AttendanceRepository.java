package com.multi.mlpenterpriseapprovalsystem.attendance.repository;

import com.multi.mlpenterpriseapprovalsystem.attendance.domain.Attendance;
import com.multi.mlpenterpriseapprovalsystem.attendance.enums.AtteType;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    /**
     * 사원의 특정 기간 근태 조회 (취소 시 삭제 대상 찾기용)
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.employee.empId = :empId " +
            "AND a.company.comId = :comId " +
            "AND a.type = :type " +
            "AND a.startAt = :startAt " +
            "AND a.endAt = :endAt")
    List<Attendance> findByEmployeeAndPeriod(
            @Param("comId") String comId,
            @Param("empId") String empId,
            @Param("type") AtteType type,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    /**
     * 사원의 특정 기간 근태 삭제 (취소 처리용)
     */
    @Modifying
    @Query("DELETE FROM Attendance a " +
            "WHERE a.employee.empId = :empId " +
            "AND a.company.comId = :comId " +
            "AND a.type = :type " +
            "AND a.startAt = :startAt " +
            "AND a.endAt = :endAt")
    int deleteByEmployeeAndPeriod(
            @Param("comId") String comId,
            @Param("empId") String empId,
            @Param("type") AtteType type,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    /**
     * 문서 번호로 근태 조회
     */
    @Query("SELECT a FROM Attendance a WHERE a.document.docId = :docId")
    List<Attendance> findByDocId(@Param("docId") String docId);


    /**
     * 사원의 특정 타입 근태 조회
     */
    List<Attendance> findByEmployee_EmpIdAndCompany_ComIdAndType(String empId, String comId, AtteType type);

    /**
     * 휴가(V) 타입이면서 오늘 시작하거나 어제 종료된 근태 조회
     */
    @Query("SELECT a FROM Attendance a " +
            "WHERE a.type = 'V' " +
            "AND (FUNCTION('DATE', a.startAt) = :today " +
            "OR FUNCTION('DATE', a.endAt) = :yesterday)")
    List<Attendance> findByStartAtIsTodayOrEndAtIsYesterday(
            @Param("today") LocalDate today,
            @Param("yesterday") LocalDate yesterday
    );


    /**
     * 사원의 모든 근태 조회 (최신순 정렬)
     */
    List<Attendance> findByEmployee_EmpIdAndCompany_ComIdOrderByStartAtDesc(
            String empId, String comId
    );

    /**
     * 사원의 특정 타입 근태 조회 (최신순 정렬)
     */
    List<Attendance> findByEmployee_EmpIdAndCompany_ComIdAndTypeOrderByStartAtDesc(
            String empId, String comId, AtteType type
    );


    // 근태 등록 시 날짜 중복 체크: (기존 시작일 <= 새 종료일) AND (기존 종료일 >= 새 시작일)
    @Query("SELECT COUNT(a) > 0 FROM Attendance a " +
            "WHERE a.employee = :employee " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    boolean existsByEmployeeAndDateOverlap(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt);

    // 근태 수정 시 날짜 중복 체크
    @Query("SELECT COUNT(a) > 0 FROM Attendance a " +
            "WHERE a.employee = :employee AND a.atteNo != :excludeAtteNo " +
            "AND a.startAt <= :endAt AND a.endAt >= :startAt")
    boolean existsByEmployeeAndDateOverlapExcludeSelf(
            @Param("employee") Employee employee,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt,
            @Param("excludeAtteNo") Long excludeAtteNo);
}
