package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.repository;

import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정(개인/부서/회사) 관련 통합 레포지토리
 *
 * @author : 권지영, 김승기
 * @filename : ScheduleRepository
 * @since : 2025. 12. 30. 화요일
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * 1. 개인 일정 조회 (scope = 'PERSONAL')
     * - 등록자(reg_emp)가 본인이고 scope가 PERSONAL인 일정만 조회
     */
    @Query("""
        select s
        from Schedule s
        where s.register.empId = :empId
          and s.scope = com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope.PERSONAL
          and s.startAt < :toExclusive
          and s.endedAt > :fromInclusive
        order by s.startAt asc, s.schNo asc
    """)
    List<Schedule> findPersonalOverlapping(
            @Param("empId") String empId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    /**
     * 2. 회사 일정 조회 (scope = 'COMPANY')
     * - 기존 'department is null' 대신 scope 필드를 명확하게 체크
     */
    @Query("""
        select s
        from Schedule s
        where s.company.comId = :comId
          and s.scope = com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope.COMPANY
          and s.startAt < :toExclusive
          and s.endedAt > :fromInclusive
        order by s.startAt asc, s.schNo asc
    """)
    List<Schedule> findCompanyOverlapping(
            @Param("comId") String comId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    /**
     * 3. 부서 일정 조회 (scope = 'DEPARTMENT')
     * - 소속 회사와 부서 번호가 일치하고 scope가 DEPARTMENT인 일정 조회
     */
    @Query("""
        select s
        from Schedule s
        where s.company.comId = :comId
          and s.department.depNo = :depNo
          and s.scope = com.multi.mlpenterpriseapprovalsystem.schedule.calendar.enums.CalendarScope.DEPARTMENT
          and s.startAt < :toExclusive
          and s.endedAt > :fromInclusive
        order by s.startAt asc, s.schNo asc
    """)
    List<Schedule> findDepartmentOverlapping(
            @Param("comId") String comId,
            @Param("depNo") Long depNo,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );
}