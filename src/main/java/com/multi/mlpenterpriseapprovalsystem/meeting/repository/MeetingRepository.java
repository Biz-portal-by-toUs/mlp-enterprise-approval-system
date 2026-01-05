package com.multi.mlpenterpriseapprovalsystem.meeting.repository;

import com.multi.mlpenterpriseapprovalsystem.meeting.domain.Meeting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 회의 repository
 *
 * @author : 김승기
 * @filename : MeetingRepository
 * @since : 2025. 12. 22. 월요일
 */
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    Optional<Meeting> findByMeetNoAndIsDeletedFalse(Long meetNo);

    /**
     * [전체회의 탭]
     * - 공개(status=true) 회의는 전체 노출
     * - 비공개(status=false)여도 내가 참여(작성자 or 참석자)이면 노출
     * - 제목검색, startedAt 날짜검색 적용
     * - depNo(부서검색)는 "회의가 해당 부서로 공개된(meeting_dept 존재)" 회의만 필터링
     */
    @Query("""
        select distinct m
        from Meeting m
        where m.company.comId = :comId
          and m.isDeleted = false
          and (
                m.status = true
                or m.writer.empId = :empId
                or exists (
                    select 1
                    from MeetingEmp me
                    where me.meeting = m
                      and me.employee.empId = :empId
                )
          )
          and (:keyword is null or :keyword = '' or lower(m.title) like lower(concat('%', :keyword, '%')))
          and (:from is null or m.startedAt >= :from)
          and (:toEx is null or m.startedAt < :toEx)
          and (
                :depNo is null
                or exists (
                    select 1
                    from MeetingDept md
                    where md.meeting = m
                      and md.department.depNo = :depNo
                )
          )
        """)
    Page<Meeting> findAllTabMeetings(
            @Param("comId") String comId,
            @Param("empId") String empId,
            @Param("depNo") Long depNo,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("toEx") LocalDateTime toEx,
            Pageable pageable
    );

    /**
     * [휴지통 탭]
     * - 내가 작성자이면서 삭제된(isDeleted=true) 회의만 노출
     * - 제목검색, startedAt 날짜검색 적용
     */
    @Query("""
    select m
    from Meeting m
    where m.company.comId = :comId
      and m.isDeleted = true
      and m.writer.empId = :empId
      and (:keyword is null or :keyword = '' or lower(m.title) like lower(concat('%', :keyword, '%')))
      and (:from is null or m.startedAt >= :from)
      and (:toEx is null or m.startedAt < :toEx)
    """)
    Page<Meeting> findDeletedMeetings(
            @Param("comId") String comId,
            @Param("empId") String empId,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("toEx") LocalDateTime toEx,
            Pageable pageable
    );

    /**
     * [내 부서 회의 탭]
     * - meeting_dept에 내 부서(depNo)가 포함된 회의만
     * - 제목검색, startedAt 날짜검색 적용
     */
    @Query("""
        select distinct m
        from Meeting m
        where m.company.comId = :comId
          and m.isDeleted = false
          and exists (
              select 1
              from MeetingDept md
              where md.meeting = m
                and md.department.depNo = :myDepNo
          )
          and (:keyword is null or :keyword = '' or lower(m.title) like lower(concat('%', :keyword, '%')))
          and (:from is null or m.startedAt >= :from)
          and (:toEx is null or m.startedAt < :toEx)
        """)
    Page<Meeting> findMyDeptMeetings(
            @Param("comId") String comId,
            @Param("myDepNo") Long myDepNo,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("toEx") LocalDateTime toEx,
            Pageable pageable
    );

    /**
     * [내 회의 탭]
     * - 내가 작성자 OR 참석자인 회의만
     * - 제목검색, startedAt 날짜검색 적용
     */
    @Query("""
        select distinct m
        from Meeting m
        where m.company.comId = :comId
          and m.isDeleted = false
          and (
                m.writer.empId = :empId
                or exists (
                    select 1
                    from MeetingEmp me
                    where me.meeting = m
                      and me.employee.empId = :empId
                )
          )
          and (:keyword is null or :keyword = '' or lower(m.title) like lower(concat('%', :keyword, '%')))
          and (:from is null or m.startedAt >= :from)
          and (:toEx is null or m.startedAt < :toEx)
        """)
    Page<Meeting> findMyMeetings(
            @Param("comId") String comId,
            @Param("empId") String empId,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("toEx") LocalDateTime toEx,
            Pageable pageable
    );
}