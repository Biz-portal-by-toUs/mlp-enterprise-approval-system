package com.multi.mlpenterpriseapprovalsystem.schedule.calendar.repository;

import com.multi.mlpenterpriseapprovalsystem.schedule.calendar.domain.EmpSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 사원 개인 일정 관련 레포지토리
 *
 * @author : 권지영
 * @filename : EmpScheduleRepository
 * @since : 2025. 12. 30. 화요일
 */
public interface EmpScheduleRepository extends JpaRepository<EmpSchedule, Long> {

    @Query("""
        select s
        from EmpSchedule s
        where s.employee.empId = :empId
          and s.startAt < :toExclusive
          and s.endedAt > :fromInclusive
        order by s.startAt asc, s.schNo asc
    """)
    List<EmpSchedule> findOverlapping(
            @Param("empId") String empId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );
}
