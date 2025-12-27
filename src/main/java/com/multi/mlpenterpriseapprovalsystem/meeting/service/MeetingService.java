package com.multi.mlpenterpriseapprovalsystem.meeting.service;

import com.multi.mlpenterpriseapprovalsystem.common.client.MeetingAiClient;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.Meeting;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingEmp;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingScope;
import com.multi.mlpenterpriseapprovalsystem.meeting.dto.*;
import com.multi.mlpenterpriseapprovalsystem.meeting.repository.MeetingDeptRepository;
import com.multi.mlpenterpriseapprovalsystem.meeting.repository.MeetingEmpRepository;
import com.multi.mlpenterpriseapprovalsystem.meeting.repository.MeetingRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 회의 서비스
 *
 * @author : 김승기
 * @filename : MeetingService
 * @since : 2025. 12. 22. 월요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingEmpRepository meetingEmpRepository;
    private final MeetingDeptRepository meetingDeptRepository;

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;

    private final MeetingAiClient meetingAiClient;

    private final EntityManager em;


    /**
     * [회의 목록 조회]
     * - scope(탭) 기준으로 데이터셋 결정
     *   1) ALL: 공개(status=true) + 비공개라도 내가 참여(작성자/참석자)면 노출
     *   2) MY_DEPT: 내 부서가 포함된 회의만
     *   3) MY: 내가 참여한 회의만
     *
     * - keyword(제목검색), 날짜(from~to: startedAt 기준) 는 모든 탭에 공통 적용
     * - depNo(부서검색)는 전체회의 탭(ALL)에서만 적용(다른 탭이면 무시)
     *
     * - 날짜 처리 규칙:
     *   - fromDate만 오면 그 날짜 하루만
     *   - toDate만 오면 그 날짜 하루만
     *   - 둘 다 오면 from~to 범위 (from > to면 스왑)
     *   - 필터는 startedAt 기준:  from <= startedAt < (to+1일 00:00)
     */
    @Transactional(readOnly = true)
    public ResMeetingListDto getMeetingList(String empId,
                                            MeetingScope scope,
                                            String keyword,
                                            Long depNo,
                                            LocalDate fromDate,
                                            LocalDate toDate,
                                            Pageable pageable) {

        Employee me = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        String comId = me.getCompany().getComId();
        Long myDepNo = (me.getDepartment() != null) ? me.getDepartment().getDepNo() : null;

        // 날짜 정규화 (from만/ to만 들어오는 경우 "하루 조회"로 처리)
        LocalDate f = fromDate;
        LocalDate t = toDate;

        if (f != null && t == null) t = f;
        if (f == null && t != null) f = t;
        if (f != null && t != null && f.isAfter(t)) {
            LocalDate tmp = f;
            f = t;
            t = tmp;
        }

        LocalDateTime from = (f != null) ? f.atStartOfDay() : null;
        LocalDateTime toExclusive = (t != null) ? t.plusDays(1).atStartOfDay() : null;

        Page<Meeting> page;

        if (scope == MeetingScope.MY) {
            // 내 회의 탭
            page = meetingRepository.findMyMeetings(comId, empId, keyword, from, toExclusive, pageable);

        } else if (scope == MeetingScope.MY_DEPT) {
            // 내 부서 회의 탭
            if (myDepNo == null) {
                page = Page.empty(pageable);
            } else {
                page = meetingRepository.findMyDeptMeetings(comId, myDepNo, keyword, from, toExclusive, pageable);
            }

        } else {
            // 전체회의 탭 (depNo 필터는 ALL에서만 적용)
            page = meetingRepository.findAllTabMeetings(comId, empId, depNo, keyword, from, toExclusive, pageable);
        }

        List<ResMeetingSimpleDto> items = page.getContent().stream()
                .map(m -> ResMeetingSimpleDto.builder()
                        .meetNo(m.getMeetNo())
                        .title(m.getTitle())
                        .startAt(m.getStartedAt())   // Meeting.getStartAt() = startedAt
                        .endAt(m.getCreatedAt())
                        .writerEmpId(m.getWriter().getEmpId())
                        .writerName(m.getWriter().getEmpName())
                        .build())
                .toList();

        return ResMeetingListDto.builder()
                .meetings(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    /**
     * 3) getMeetingDetail() : 비공개면 권한 체크(참석자)
     */
    @Transactional(readOnly = true)
    public ResMeetingDetailDto getMeetingDetail(String empId, Long meetNo) {

        Employee viewer = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Meeting meeting = meetingRepository.findByMeetNoAndIsDeletedFalse(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getCompany().getComId().equals(viewer.getCompany().getComId())) {
            throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
        }

        if (Boolean.FALSE.equals(meeting.getStatus())) {
            boolean isWriter = meeting.getWriter().getEmpId().equals(empId);
            boolean isParticipant = meetingEmpRepository.existsByMeeting_MeetNoAndEmployee_EmpId(meetNo, empId);


            if (!isWriter && !isParticipant) {
                throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
            }
        }

        List<MeetingEmp> memps = meetingEmpRepository.findAllByMeeting_MeetNo(meetNo);
        List<ResMeetingParticipantDto> participants = new ArrayList<>();
        for (MeetingEmp me : memps) {
            participants.add(ResMeetingParticipantDto.builder()
                    .empId(me.getEmployee().getEmpId())
                    .empName(me.getEmployee().getEmpName())
                    .build());
        }


        Long depNo = null;
        String depName = null;
        if (meeting.getMeetingDepts() != null && !meeting.getMeetingDepts().isEmpty()) {
            Department d = meeting.getMeetingDepts().get(0).getDepartment();
            depNo = d.getDepNo();
            depName = d.getDepName();
        }

        return ResMeetingDetailDto.builder()
                .meetNo(meeting.getMeetNo())
                .title(meeting.getTitle())
                .sttText(meeting.getSttText())
                .aiText(meeting.getAiText())
                .startAt(meeting.getStartedAt())
                .endAt(meeting.getCreatedAt()) // 너가 endAt을 createdAt로 쓰는 구조면 유지
                .depNo(depNo)
                .depName(depName)
                .writerEmpId(meeting.getWriter().getEmpId())
                .writerName(meeting.getWriter().getEmpName())
                .participants(participants)
                .build();
    }

    @Transactional
    public Long createMeeting(String empId, ReqMeetingCreateDto request) {

        Employee writer = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Company company = companyRepository.findByComId(writer.getCompany().getComId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 부서(depNos)는 항상 1개 이상
        if (request.getDepNos() == null || request.getDepNos().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_DEPT_REQUIRED);
        }

        // 참석자(participantEmpIds)는 항상 1명 이상
        if (request.getParticipantEmpIds() == null || request.getParticipantEmpIds().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_PARTICIPANT_REQUIRED);
        }

        Meeting meeting = Meeting.create(
                request.getTitle(),
                request.getStartedAt(),
                company,
                writer,
                request.getStatus()
        );

        // depNos 중복 제거 후 저장
        for (Long dno : request.getDepNos().stream().distinct().toList()) {
            Department dep = departmentRepository.findById(dno)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
            meeting.addDepartment(dep);
        }

        Meeting saved = meetingRepository.save(meeting);

        // 참석자 중복 제거 후 저장
        List<String> distinctEmpIds = request.getParticipantEmpIds().stream()
                .distinct()
                .toList();

        boolean writerIncluded = distinctEmpIds.contains(writer.getEmpId());

        for (String pid : distinctEmpIds) {
            Employee p = employeeRepository.findByEmpId(pid)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

            // meeting_emp에 com_id 없는 버전
            meetingEmpRepository.save(MeetingEmp.create(saved, p));
        }

        // 작성자도 참석자에 자동 포함(항상)
        if (!writerIncluded) {
            meetingEmpRepository.save(MeetingEmp.create(saved, writer));
        }

        return saved.getMeetNo();
    }
    @Transactional
    public Long updateMeeting(String empId, Long meetNo, ReqMeetingUpdateDto request) {

        Meeting meeting = meetingRepository.findByMeetNoAndIsDeletedFalse(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        // 작성자만 수정 가능
        if (!meeting.getWriter().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
        }

        if (request.getDepNos() == null || request.getDepNos().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_DEPT_REQUIRED);
        }

        if (request.getParticipantEmpIds() == null || request.getParticipantEmpIds().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_PARTICIPANT_REQUIRED);
        }

        meeting.updateBasic(
                request.getTitle(),
                request.getStartedAt(),
                request.getStatus(),
                request.getSttText(),
                request.getAiText()
        );

        // 부서 교체: "DB에서 먼저 delete → flush → 컬렉션 비우기 → add"
        meetingDeptRepository.deleteAllByMeeting_MeetNo(meetNo);
        em.flush();                 // ✅ DB delete 먼저 확정
        meeting.clearDepartments(); // ✅ 영속성 컨텍스트 컬렉션도 정리(중요)

        for (Long dno : request.getDepNos().stream().distinct().toList()) {
            Department dep = departmentRepository.findById(dno)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
            meeting.addDepartment(dep);
        }

        meetingEmpRepository.deleteAllByMeeting_MeetNo(meetNo);
        em.flush();

        List<String> distinctEmpIds = request.getParticipantEmpIds().stream()
                .distinct()
                .toList();

        String writerId = meeting.getWriter().getEmpId();
        boolean writerIncluded = distinctEmpIds.contains(writerId);

        for (String pid : distinctEmpIds) {
            Employee p = employeeRepository.findByEmpId(pid)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
            meetingEmpRepository.save(MeetingEmp.create(meeting, p)); // com_id 없는 버전
        }

        if (!writerIncluded) {
            meetingEmpRepository.save(MeetingEmp.create(meeting, meeting.getWriter()));
        }

        return meeting.getMeetNo();
    }



    @Transactional
    public Long deleteMeeting(String empId, Long meetNo) {
        Meeting meeting = meetingRepository.findByMeetNoAndIsDeletedFalse(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getWriter().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.MEETING_EDIT_DELETE_DENIED);
        }

        meeting.delete();
        meetingEmpRepository.deleteAllByMeeting_MeetNo(meetNo);
        meetingDeptRepository.deleteAllByMeeting_MeetNo(meetNo);

        return meeting.getMeetNo();
    }

    // ✅ 프론트가 AI 처리 요청
    @Transactional
    public void requestAiPipeline(String empId, Long meetNo, ReqMeetingAiRequestDto req) {

        Meeting meeting = meetingRepository.findByMeetNoAndIsDeletedFalse(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        // 작성자만 요청 가능(원하면 참석자도 허용으로 바꾸면 됨)
        if (!meeting.getWriter().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
        }

        // (선택) 처리 상태값 컬럼이 있으면 PROCESSING으로 변경
         meeting.markAiProcessing();

        // ✅ Spring -> FastAPI 호출 (비동기 권장)
        meetingAiClient.requestAi(meetNo, req.getObjectKey());
    }

    // MeetingService.java 안에 추가
    @Transactional
    public Long applyAiResult(Long meetNo, ReqMeetingAiCallbackDto request) {

        Meeting meeting = meetingRepository.findByMeetNoAndIsDeletedFalse(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        String status = request.getStatus() == null ? "" : request.getStatus();
        log.info("CALLBACK sttText len={}, aiText len={}, status={}",
                request.getSttText() == null ? -1 : request.getSttText().length(),
                request.getAiText() == null ? -1 : request.getAiText().length(),
                request.getStatus());

        if ("FAILED".equalsIgnoreCase(status)) {
            // ⚠️ DTO 필드명이 errorMessage면 여기 맞춰야 함
            meeting.markAiFailed(request.getErrorMessage());  // request.getError() 쓰면 안 맞을 수 있음
            return meeting.getMeetNo();
        }

        // DONE
        meeting.markAiDone(request.getSttText(), request.getAiText());

        // 디버깅용: 바로 DB 반영 확인하고 싶으면
        // meetingRepository.saveAndFlush(meeting);

        return meeting.getMeetNo();
    }
}