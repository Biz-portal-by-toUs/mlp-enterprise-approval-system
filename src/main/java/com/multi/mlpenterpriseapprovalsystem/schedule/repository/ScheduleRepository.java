package com.multi.mlpenterpriseapprovalsystem.schedule.repository;

import com.multi.mlpenterpriseapprovalsystem.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회사/부서 일정 관련 레포지토리
 *
 * @author : 권지영
 * @filename : ScheduleRepository
 * @since : 2025. 12. 30. 화요일
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 회사 일정(depNo is null)
    @Query("""
    select s
    from Schedule s
    where s.company.comId = :comId
      and s.department is null
      and s.startAt < :toExclusive
      and s.endedAt > :fromInclusive
    order by s.startAt asc, s.schNo asc
""")
    List<Schedule> findCompanyOverlapping(
            @Param("comId") String comId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    // 부서 일정(depNo = :depNo)
    @Query("""
        select s
        from Schedule s
        where s.department.depNo = :depNo
          and s.startAt < :toExclusive
          and s.endedAt > :fromInclusive
        order by s.startAt asc, s.schNo asc
    """)
    List<Schedule> findDepartmentOverlapping(
            @Param("depNo") Long depNo,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );
}
