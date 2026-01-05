package com.multi.mlpenterpriseapprovalsystem.meeting.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.S3UrlService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.Meeting;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingDept;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingEmp;
import com.multi.mlpenterpriseapprovalsystem.meeting.domain.MeetingScope;
import com.multi.mlpenterpriseapprovalsystem.meeting.dto.*;
import com.multi.mlpenterpriseapprovalsystem.meeting.event.MeetingAiRequestedEvent;
import com.multi.mlpenterpriseapprovalsystem.meeting.repository.MeetingDeptRepository;
import com.multi.mlpenterpriseapprovalsystem.meeting.repository.MeetingEmpRepository;
import com.multi.mlpenterpriseapprovalsystem.meeting.repository.MeetingRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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

    private final S3UrlService s3UrlService;
    private final EntityManager em;

    private final ApplicationEventPublisher publisher;

    private final NotificationService notificationService;

    /**
     * [회의 목록 조회]
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

        LocalDate f = fromDate;
        LocalDate t = toDate;

        if (f != null && t != null && f.isAfter(t)) {
            LocalDate tmp = f;
            f = t;
            t = tmp;
        }

        LocalDateTime from = (f != null) ? f.atStartOfDay() : null;
        LocalDateTime toExclusive = (t != null) ? t.plusDays(1).atStartOfDay() : null;

        Page<Meeting> page;

        if (scope == MeetingScope.MY) {
            page = meetingRepository.findMyMeetings(comId, empId, keyword, from, toExclusive, pageable);
        } else if (scope == MeetingScope.MY_DEPT) {
            if (myDepNo == null) {
                page = Page.empty(pageable);
            } else {
                page = meetingRepository.findMyDeptMeetings(comId, myDepNo, keyword, from, toExclusive, pageable);
            }
        } else {
            page = meetingRepository.findAllTabMeetings(comId, empId, depNo, keyword, from, toExclusive, pageable);
        }

        List<ResMeetingSimpleDto> items = page.getContent().stream()
                .map(m -> ResMeetingSimpleDto.builder()
                        .meetNo(m.getMeetNo())
                        .title(m.getTitle())
                        .startAt(m.getStartedAt())
                        .endAt(m.getCreatedAt())
                        .aiStatus(String.valueOf(m.getAiStatus()))
                        .writerEmpId(m.getWriter().getEmpId())
                        .writerName(m.getWriter().getEmpName())
                        .participantCount(m.getMeetingEmps().size())
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

    @Transactional(readOnly = true)
    public ResMeetingListDto getDeletedMeetingList(String empId,
                                                   String keyword,
                                                   LocalDate fromDate,
                                                   LocalDate toDate,
                                                   Pageable pageable) {

        // 1. 사용자 정보 및 소속 회사 확인
        Employee me = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        String comId = me.getCompany().getComId();

        // 2. 날짜 필터링 로직 (기존 로직 유지)
        LocalDate f = fromDate;
        LocalDate t = toDate;
        if (f != null && t != null && f.isAfter(t)) {
            LocalDate tmp = f;
            f = t;
            t = tmp;
        }

        LocalDateTime from = (f != null) ? f.atStartOfDay() : null;
        LocalDateTime toExclusive = (t != null) ? t.plusDays(1).atStartOfDay() : null;

        // 3. 레포지토리 호출 (삭제된 데이터 전용 쿼리)
        // 휴지통은 보통 본인이 삭제한 것만 보거나, 전체를 보더라도 isDeleted=true 조건이 필수입니다.
        Page<Meeting> page = meetingRepository.findDeletedMeetings(comId, empId, keyword, from, toExclusive, pageable);

        // 4. DTO 변환 (기존 로직 유지)
        List<ResMeetingSimpleDto> items = page.getContent().stream()
                .map(m -> ResMeetingSimpleDto.builder()
                        .meetNo(m.getMeetNo())
                        .title(m.getTitle())
                        .startAt(m.getStartedAt())
                        .endAt(m.getCreatedAt()) // 필요 시 종료시간이나 삭제시간으로 변경 가능
                        .aiStatus(String.valueOf(m.getAiStatus()))
                        .writerEmpId(m.getWriter().getEmpId())
                        .writerName(m.getWriter().getEmpName())
                        .participantCount(m.getMeetingEmps().size())
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

        List<ResMeetingDepartmentDto> departments = new ArrayList<>();
        if (meeting.getMeetingDepts() != null) {
            for (MeetingDept md : meeting.getMeetingDepts()) {
                Department d = md.getDepartment();
                if (d == null) continue;
                departments.add(ResMeetingDepartmentDto.builder()
                        .depNo(d.getDepNo())
                        .depName(d.getDepName())
                        .build());
            }
        }

        String audioKey = meeting.getAudioObjectKey();
        String recordUrl = null;

        if (audioKey != null && !audioKey.isBlank()) {
            recordUrl = s3UrlService.presignGetUrl(audioKey);
        }

        return ResMeetingDetailDto.builder()
                .meetNo(meeting.getMeetNo())
                .title(meeting.getTitle())
                .sttText(meeting.getSttText())
                .aiText(meeting.getAiText())
                .startAt(meeting.getStartedAt())
                .endAt(meeting.getCreatedAt())
                .departments(departments)
                .status(meeting.getStatus())
                .writerEmpId(meeting.getWriter().getEmpId())
                .writerName(meeting.getWriter().getEmpName())
                .recordUrl(recordUrl)
                .objectKey(audioKey)
                .participants(participants)
                .build();
    }

    @Transactional
    public Long restoreMeeting(String empId, Long meetNo) {

        Meeting meeting = meetingRepository.findById(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getWriter().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        if (!meeting.getIsDeleted()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        // 4. 복구 수행
        meeting.setDeleted(false);
        // JPA 더티 체킹에 의해 별도의 save 없이 트랜잭션 종료 시 반영됩니다.

        return meetNo;
    }

    @Transactional
    public Long createMeeting(String empId, ReqMeetingCreateDto request) {

        Employee writer = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        Company company = companyRepository.findByComId(writer.getCompany().getComId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        if (request.getDepNos() == null || request.getDepNos().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_DEPT_REQUIRED);
        }
        if (request.getParticipantEmpIds() == null || request.getParticipantEmpIds().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_PARTICIPANT_REQUIRED);
        }

        if (request.getTitle().length()>30) {
            throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
        }

        Meeting meeting = Meeting.create(
                request.getTitle(),
                request.getStartedAt(),
                company,
                writer,
                request.getStatus()
        );

        for (Long dno : request.getDepNos().stream().distinct().toList()) {
            Department dep = departmentRepository.findById(dno)
                    .orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
            meeting.addDepartment(dep);
        }

        Meeting saved = meetingRepository.save(meeting);

        List<String> distinctEmpIds = request.getParticipantEmpIds().stream()
                .distinct()
                .toList();

        boolean writerIncluded = distinctEmpIds.contains(writer.getEmpId());

        for (String pid : distinctEmpIds) {
            Employee p = employeeRepository.findByEmpId(pid)
                    .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
            meetingEmpRepository.save(MeetingEmp.create(saved, p));
        }

        if (!writerIncluded) {
            meetingEmpRepository.save(MeetingEmp.create(saved, writer));
        }

        return saved.getMeetNo();
    }

    @Transactional
    public Long updateMeeting(String empId, Long meetNo, ReqMeetingUpdateDto request) {

        Meeting meeting = meetingRepository.findByMeetNoAndIsDeletedFalse(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getWriter().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
        }

        if (request.getDepNos() == null || request.getDepNos().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_DEPT_REQUIRED);
        }

        if (request.getParticipantEmpIds() == null || request.getParticipantEmpIds().isEmpty()) {
            throw new CustomException(ErrorCode.MEETING_PARTICIPANT_REQUIRED);
        }

        if (request.getTitle().length()>30) {
            throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
        }

        meeting.updateBasic(
                request.getTitle(),
                request.getStartedAt(),
                request.getStatus(),
                request.getSttText(),
                request.getAiText()
        );

        meetingDeptRepository.deleteAllByMeeting_MeetNo(meetNo);
        em.flush();
        meeting.clearDepartments();

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
            meetingEmpRepository.save(MeetingEmp.create(meeting, p));


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
        return meeting.getMeetNo();
    }

    /**
     * ✅ 핵심 수정: 트랜잭션 안에서는 DB 상태만 업데이트하고,
     * FastAPI 호출은 "커밋 이후" 이벤트 리스너(@TransactionalEventListener AFTER_COMMIT)에서 실행
     */
    @Transactional
    public void requestAiPipeline(String empId, Long meetNo, ReqMeetingAiRequestDto req) {

        Meeting meeting = meetingRepository.findByMeetNoAndIsDeletedFalse(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getWriter().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.MEETING_ACCESS_DENIED);
        }

        meeting.markAiProcessing();
        meeting.updateTexts("음성을 텍스트로 변환중입니다...", "회의 요약중입니다...");

        publisher.publishEvent(new MeetingAiRequestedEvent(meetNo, req.getObjectKey(), req.getTitle()));
    }

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
            meeting.markAiFailed(request.getErrorMessage());
            return meeting.getMeetNo();
        }

        meeting.markAiDone(request.getSttText(), request.getAiText());
        meeting.setAudioObjectKey(request.getObjectKey());

        // ✅ 알림 전송 로직 추가 (작성자에게 알림)
        notificationService.sendNotification(
                meeting.getWriter(),
                NotificationType.MEETING,
                "회의록 요약 완료", // UI에 제목으로 표시됨
                "\"" + meeting.getTitle() + "\" 회의의 AI 요약이 완료되었습니다.",
                "/meeting/" + meeting.getMeetNo()
        );

        return meeting.getMeetNo();
    }

    @Transactional
    public Long hardDeleteMeeting(String empId, Long meetNo) {
        Meeting meeting = meetingRepository.findById(meetNo)
                .orElseThrow(() -> new CustomException(ErrorCode.MEETING_NOT_FOUND));

        if (!meeting.getWriter().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        meetingEmpRepository.deleteAllByMeeting_MeetNo(meetNo);
        meetingDeptRepository.deleteAllByMeeting_MeetNo(meetNo);
        meetingRepository.delete(meeting);

        return meetNo;
    }
}