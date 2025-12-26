package com.multi.mlpenterpriseapprovalsystem.meeting.repository;

import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingDept;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회의 참석 부서 repository
 *
 * @author : 김승기
 * @filename : MeetingDeptRepository
 * @since : 2025. 12. 23. 화요일
 */
public interface MeetingDeptRepository extends JpaRepository<MeetingDept, Long> {

    @Modifying(flushAutomatically = true)
    @Query("delete from MeetingDept md where md.meeting.meetNo = :meetNo")
    void deleteAllByMeeting_MeetNo(@Param("meetNo") Long meetNo);

    boolean existsByMeeting_MeetNoAndDepartment_DepNo(Long meetNo, Long depNo);
}