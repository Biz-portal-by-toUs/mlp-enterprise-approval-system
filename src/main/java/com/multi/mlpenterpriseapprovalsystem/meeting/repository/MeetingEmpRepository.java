package com.multi.mlpenterpriseapprovalsystem.meeting.repository;

import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingEmp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 회의 참석자 부서 repository
 *
 * @author : 김승기
 * @filename : MeetingEmpRepository
 * @since : 2025. 12. 22. 월요일
 */
public interface MeetingEmpRepository extends JpaRepository<MeetingEmp, Long> {

    List<MeetingEmp> findAllByMeeting_MeetNo(Long meetNo);

    boolean existsByMeeting_MeetNoAndEmployee_EmpId(Long meetNo, String empId);

    @Modifying(flushAutomatically = true)
    @Query("delete from MeetingEmp me where me.meeting.meetNo = :meetNo")
    void deleteAllByMeeting_MeetNo(@Param("meetNo") Long meetNo);
}