package com.multi.mlpenterpriseapprovalsystem.attendance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.attendance.domain.Attendance;
import com.multi.mlpenterpriseapprovalsystem.attendance.dto.res.ResAttendanceDto;
import com.multi.mlpenterpriseapprovalsystem.attendance.enums.AtteType;
import com.multi.mlpenterpriseapprovalsystem.attendance.repository.AttendanceRepository;
import com.multi.mlpenterpriseapprovalsystem.attendance.schedule.AttendanceSchedule;
import com.multi.mlpenterpriseapprovalsystem.attendance.tiptap_json.AttendanceInfo;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.document.domain.Document;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 근태 관리 서비스
 *
 * @author : 이지헌
 * @filename : AttendanceService
 * @since : 25. 12. 29. 월요일
 */
@Slf4j
@Service
@Transactional
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ObjectMapper objectMapper;
    private final AttendanceSchedule attendanceSchedule;
    private final NotificationService notificationService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             @Qualifier("objectMapper") ObjectMapper objectMapper,
                             AttendanceSchedule attendanceSchedule, NotificationService notificationService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.objectMapper = objectMapper;
        this.attendanceSchedule = attendanceSchedule;
        this.notificationService = notificationService;
    }

    /*
    * 문서 최종승인 시 근태 처리
    * - 휴가신청/출장신청 → 근태 등록
    * - 휴가수정신청/출장수정신청 → 근태 수정
    * - 휴가취소신청/출장취소신청 → 근태 삭제
    */
    public void processAttendance(Document document) {
        String docfoName = document.getDocumentForm().getDocfoName();

        // 휴가 신청서 또는 출장 신청서 양식이 아니면 스킵
        if (!docfoName.equals("출장 신청서") && !docfoName.equals("휴가 신청서")) {
            log.info("휴가 신청서, 출장 신청서가 아님. 근태 관리 스킵");
            return;
        }

        // content에서 attendanceInfo 파싱
        AttendanceInfo info = parseAttendanceInfo(document.getContent());
        if (info == null) {
            log.warn("문서에 attendanceInfo가 없습니다. docNo={}", document.getDocNo());
            return;
        }

        // 카테고리명으로 신청/수정/취소 분기
        String categoryName = document.getDocumentFormCategory().getName();
        AtteType type = (docfoName.equals("휴가 신청서")) ? AtteType.V : AtteType.B;

        if (categoryName.contains("취소")) {
            // 휴가취소신청 또는 출장취소신청 → 삭제
            cancelAttendance(document, info);
        } else if (categoryName.contains("수정")) {
            // 휴가수정신청 또는 출장수정신청 → 수정
            modifyAttendance(document, info);
        } else if (categoryName.contains("신청") && !categoryName.contains("취소") && !categoryName.contains("수정")) {
            // 휴가신청 또는 출장신청 → 등록
            createAttendance(document, type, info);
        } else {
            log.warn("알 수 없는 카테고리: {}", categoryName);
        }
    }

    // 근태 등록
    // 휴가: 근태 추가 -> 휴가자의 대직자 갱신(시작일 = 오늘) -> 결재라인에 대직자 추가(시작일 = 오늘)
    // 출장: 근태 추가
    private void createAttendance(Document document, AtteType type, AttendanceInfo info) {

        Company company = document.getCompany();
        Employee writer = document.getWriter();

        // 근태 시작일은 오늘, 미래만 가능. 근태 종료일은 시작일부터 미래까지 가능
        LocalDate startDate = info.getStartAt().toLocalDate();
        LocalDate endDate = info.getEndAt().toLocalDate();
        LocalDate today = LocalDate.now();

        // 근태 시작일은 오늘, 미래만 가능
        if(startDate.isBefore(today)){
            throw new CustomException(ErrorCode.START_DATE_MUST_BE_TODAY_OR_LATER);
        }

        // 근태 종료일은 시작일부터 미래까지 가능
        if(endDate.isBefore(startDate)) {
            throw new CustomException(ErrorCode.END_DATE_BEFORE_START_DATE);
        }

        // 등록할 근태가 이미 등록된 다른 근태와 날짜가 겹치는지 체크
        if (attendanceRepository.existsByEmployeeAndDateOverlapAndIsDeletedFalse(writer, info.getStartAt(), info.getEndAt())) {
            throw new CustomException(ErrorCode.ATTENDANCE_ALREADY_EXISTS_IN_PERIOD);
        }

        // 일 수 계산
        int days = (int) ChronoUnit.DAYS.between(info.getStartAt().toLocalDate(), info.getEndAt().toLocalDate()) + 1;

        // 대직자 조회 (있으면)
        Employee delegate = null;
        if (info.getDelegateEmpId() != null && !info.getDelegateEmpId().isEmpty()) {
            delegate = employeeRepository.findByEmpId(info.getDelegateEmpId())
                    .orElse(null);
        }

        // 휴가이면서 대직자가 있는 경우
        if(delegate != null && document.getDocumentForm().getDocfoName().equals("휴가 신청서")) {
            // 휴가중인 사람은 대직자로 선택 불가능 todo: 주석풀어야하는지 체크
//            if(delegate.getAtte().equals("V")){
//                throw new CustomException(ErrorCode.DELEGATE_IS_ON_VACATION);
//            }

            // 내가 휴가인 기간에 대직자도 휴가 일정이 있는지 체크
            if (attendanceRepository.existsByEmployeeAndDateOverlapAndTypeIsVAndIsDeletedFalse(delegate, info.getStartAt(), info.getEndAt(), AtteType.V)) {
                throw new CustomException(ErrorCode.DELEGATE_ALREADY_HAS_LEAVE_IN_PERIOD);
            }

            // 같은 부서의 사람만 대직자로 선택 가능
            if(!delegate.getDepartment().getDepName().equals(writer.getDepartment().getDepName())) {
                throw new CustomException(ErrorCode.DELEGATE_DIFF_DEPARTMENT);
            }
        }

        // 근태 테이블에 등록
        Attendance attendance = Attendance.builder()
                .company(company)
                .employee(writer)
                .document(document)
                .type(type)
                .day(days)
                .delegate(delegate)
                .startAt(info.getStartAt())
                .endAt(info.getEndAt())
                .build();

        attendanceRepository.save(attendance);
        document.linkAttendance(attendance); // 문서와 근태 연결 (이력 생성)

        // 대직자 알림 추가
        if (delegate != null) {
            notifyDelegate(writer, delegate, document, "[대직 지정]", "님의 대직자로 지정되었습니다.");
        }

        log.info("근태 등록 완료: empId={}, type={}, period={} ~ {}, days={}",
                writer.getEmpId(), type, info.getStartAt(), info.getEndAt(), days);

        if (info.getStartAt().toLocalDate().isEqual(LocalDate.now())) {
            String atteType = (type == AtteType.V) ? "V" : "B";

            // 사원 상태 업데이트 (atte = V/B, msgStat = H)
            writer.updateAtteStatus(atteType);

            if (type == AtteType.V) {
                if(delegate == null){
                    writer.updateDelegate(null);
                } else {
                    writer.updateDelegate(delegate);
                    attendanceSchedule.addDelegateToApprovalLines(writer, delegate);
                }
            }
            log.info("오늘 시작되는 근태이므로 사원 상태를 '{}'로 즉시 변경", atteType);
        }
    }

    /*
    * 근태 수정
    * 원본 시작일이 과거: 시작일은 변경 불가, 종료일은 어제부터 미래까지 가능
    * 원본 시작일이 오늘, 미래: 시작은 오늘부터 미래까지 가능, 종료일은 시작일부터 미래까지 가능
    * 휴가: 기존 근태 수정 ->
    *      결재중인 문서의 결재라인에서 휴가자와 같은 결재순서이며 결재상태가 결재중, 결재대기중인 기존 대직자 삭제 ->
    *      휴가자의 기존 대직자를 새 대직자로 수정 ->
    *      결재라인에 새 대직자 추가
    *
    * 출장: 기존 근태 수정
    */
    private void modifyAttendance(Document document, AttendanceInfo info) {
        // targetAtteNo 검증
        if (info.getTargetAtteNo() == null) {
            throw new CustomException(ErrorCode.TARGET_ATTENDANCE_NOT_SPECIFIED);
        }

        // 대상 근태 조회
        Attendance targetAttendance = attendanceRepository.findById(info.getTargetAtteNo())
                .orElseThrow(() -> new CustomException(ErrorCode.ATTENDANCE_NOT_FOUND));

        // 본인 확인
        if (!targetAttendance.getEmployee().getEmpId().equals(document.getWriter().getEmpId())) {
            throw new CustomException(ErrorCode.ATTENDANCE_ACCESS_DENIED);
        }

        // 날짜 검증
        validateModification(targetAttendance, info.getStartAt(), info.getEndAt());

        // 수정된 후의 근태가 이미 등록된 다른 근태와 날짜가 겹치는지 체크(대상 근태 제외)
        boolean isOverlapped = attendanceRepository.existsByEmployeeAndDateOverlapExcludeSelfAndIsDeletedFalse(
                targetAttendance.getEmployee(),
                info.getStartAt(),
                info.getEndAt(),
                targetAttendance.getAtteNo() // 대상 근태 제외
        );

        if (isOverlapped) {
            throw new CustomException(ErrorCode.ATTENDANCE_ALREADY_EXISTS_IN_PERIOD);
        }

        LocalDate today = LocalDate.now();
        Employee writer = document.getWriter();
        Employee oldDelegate = targetAttendance.getDelegate();
        Employee newDelegate = null;
        if (targetAttendance.getType() == AtteType.V) {
            if (info.getDelegateEmpId() != null && !info.getDelegateEmpId().isEmpty()) {
                newDelegate = employeeRepository.findByEmpId(info.getDelegateEmpId()).orElse(null);
            }
        }

        if (newDelegate != null) {
            // 휴가 중인 사람은 대직자로 선택 불가능 todo: 주석풀어야할지 체크
//            if ("V".equals(newDelegate.getAtte())) {
//                throw new CustomException(ErrorCode.DELEGATE_IS_ON_VACATION);
//            }
            if (newDelegate != null && attendanceRepository.existsByEmployeeAndDateOverlapAndTypeIsVAndIsDeletedFalse(newDelegate, info.getStartAt(), info.getEndAt(), AtteType.V)) {
                throw new CustomException(ErrorCode.DELEGATE_ALREADY_HAS_LEAVE_IN_PERIOD);
            }

            // 같은 부서 사람만 가능
            if (!newDelegate.getDepartment().getDepName().equals(writer.getDepartment().getDepName())) {
                throw new CustomException(ErrorCode.DELEGATE_DIFF_DEPARTMENT);
            }
        }

        // 일수 재계산
        int newDays = (int) ChronoUnit.DAYS.between(
                info.getStartAt().toLocalDate(),
                info.getEndAt().toLocalDate()
        ) + 1;

        // 수정 전후의 "오늘 휴가 여부" 판단
        boolean wasActiveToday = !targetAttendance.getStartAt().toLocalDate().isAfter(today)
                && !targetAttendance.getEndAt().toLocalDate().isBefore(today);

        boolean isActiveTodayNow = !info.getStartAt().toLocalDate().isAfter(today)
                && !info.getEndAt().toLocalDate().isBefore(today);

        // 기존 근태 정보 수정
        targetAttendance.modifyDates(info.getStartAt(), info.getEndAt(), newDays); // 근태의 시간정보 갱신
        targetAttendance.updateRecentDocument(document); // 근태 엔티티의 '최근 문서' 필드도 현재의 수정 문서로 교체


        // 대직자 변경 알림 추가
        if (!java.util.Objects.equals(oldDelegate, newDelegate)) {
            // 기존 대직자에게 해제 알림
            if (oldDelegate != null) {
                notifyDelegate(writer, oldDelegate, document, "[대직 해제]", "님의 대직 지정이 취소(변경)되었습니다.");
            }
            // 새 대직자에게 지정 알림
            if (newDelegate != null) {
                notifyDelegate(writer, newDelegate, document, "[대직 지정]", "님의 대직자로 지정되었습니다.");
            }
        }

        // 근태 엔티티의 대직자 정보도 새 대직자로 갱신해줘야 함!
        if (targetAttendance.getType() == AtteType.V) {
            targetAttendance.updateDelegate(newDelegate);
        }
        document.linkAttendance(targetAttendance); // 수정 문서도 해당 근태에 연결

        if (wasActiveToday && !isActiveTodayNow) {
            // 오늘 활성이었는데 아니게 됨 -> 출근(C)으로 복구
            writer.updateAtteStatus("C");
            log.info("근태 수정: 오늘 근태 해제로 인해 사원 상태 'C'로 복구");
        }
        else if (!wasActiveToday && isActiveTodayNow) {
            // 오늘 활성이 아니었는데 활성이 됨 -> V 또는 B로 변경
            String atteType = (targetAttendance.getType() == AtteType.V) ? "V" : "B";
            writer.updateAtteStatus(atteType);
            log.info("근태 수정: 오늘 근태 시작으로 인해 사원 상태 '{}'로 변경", atteType);
        }

        if (targetAttendance.getType() == AtteType.V) {
            // 상황 A: 오늘 휴가였는데, 이제 아니게 된 경우 (조기 복귀 / 미래로 연기)
            // 대직자가 누구로 바뀌었든 상관없이, '오늘' 기준으로는 대직자 권한을 회수해야 함
            if (wasActiveToday && !isActiveTodayNow) {
                if (oldDelegate != null) {
                    attendanceSchedule.removeDelegateFromApprovalLines(writer, oldDelegate);
                }
                writer.updateDelegate(null);
                log.info("근태 수정: 오늘 휴가 해제로 인한 대직자 제거 완료");
            }

            // 상황 B: 원래 오늘 휴가가 아니었는데, 오늘부터 휴가인 상태가 됨 (시작일 당겨짐)
            // 이때 새로 선택된 newDelegate를 즉시 투입함
            else if (!wasActiveToday && isActiveTodayNow) {
                writer.updateDelegate(newDelegate);
                if (newDelegate != null) {
                    attendanceSchedule.addDelegateToApprovalLines(writer, newDelegate);
                }
                log.info("근태 수정: 오늘 휴가 시작으로 인한 대직자 추가 완료");
            }

            // 상황 C: 수정 전후 모두 '오늘 휴가 중'인 상태 (대직자 변경 체크)
            // null -> 객체, 객체 -> null, 객체A -> 객체B 모든 경우를 체크함
            else if (wasActiveToday && isActiveTodayNow) {
                if (!java.util.Objects.equals(oldDelegate, newDelegate)) {
                    // 1. 기존 대직자가 있었다면 결재라인에서 제거
                    if (oldDelegate != null) {
                        attendanceSchedule.removeDelegateFromApprovalLines(writer, oldDelegate);
                    }
                    // 2. 사원 테이블 정보 갱신
                    writer.updateDelegate(newDelegate);
                    // 3. 새 대직자가 있다면 결재라인에 추가
                    if (newDelegate != null) {
                        attendanceSchedule.addDelegateToApprovalLines(writer, newDelegate);
                    }
                    log.info("근태 수정: 오늘 휴가 중 대직자 변경 완료");
                }
            }
        }
    }

    /*
    * 근태 삭제 (취소 처리)
    * 근태 시작일이 오늘, 미래일 때만 취소 가능
    * 근태시작일이 과거면 수정만 가능
    * 휴가 취소: 결재라인에서 삭제 -> 휴가자의 대직자 null로 변경 -> 근태 삭제
    * 출장 취소: 근태 삭제
    *
    */
    private void cancelAttendance(Document document, AttendanceInfo info) {
        // targetAtteNo 검증
        if (info.getTargetAtteNo() == null) {
            throw new CustomException(ErrorCode.TARGET_ATTENDANCE_NOT_SPECIFIED);
        }

        // 대상 근태 조회
        Attendance targetAttendance = attendanceRepository.findById(info.getTargetAtteNo())
                .orElseThrow(() -> new CustomException(ErrorCode.ATTENDANCE_NOT_FOUND));

        // 본인 확인
        if (!targetAttendance.getEmployee().getEmpId().equals(document.getWriter().getEmpId())) {
            throw new CustomException(ErrorCode.ATTENDANCE_ACCESS_DENIED);
        }

        // 시작일이 오늘, 미래일때만 근태 취소 가능
        LocalDate startDate = targetAttendance.getStartAt().toLocalDate();
        LocalDate today = LocalDate.now();
        if (startDate.isBefore(today)) {
            throw new CustomException(ErrorCode.CANNOT_CANCEL_PAST_ATTENDANCE);
        }

        // 휴가자
        Employee writer = document.getWriter();
        // 대직자
        Employee delegate = targetAttendance.getDelegate();

        // 대직자 해제 알림 추가
        if (delegate != null) {
            notifyDelegate(writer, delegate, document, "[대직 해제]", "님의 근태 취소로 대직 지정이 해제되었습니다.");
        }

        // 휴가이며, 대직자가 있고, 시작일이 오늘이면 결재라인에서 제거
        if(targetAttendance.getType() == AtteType.V && delegate != null && startDate.equals(today)) {
            attendanceSchedule.removeDelegateFromApprovalLines(writer, delegate);
        }

        // 근태타입, 대직자 유무와 상관없이,
        // 시작일이 오늘이면 사원테이블에서 대직자를 null로 설정
        if(startDate.equals(today)) {
            writer.updateDelegate(null);
            writer.updateAtteStatus("C"); // 즉시 출근(C) 상태로 원복
        }

        // 근태 삭제
        targetAttendance.softDelete();

        // 이력 보존을 위해 문서와 연결 유지
        document.linkAttendance(targetAttendance);

        log.info("근태 취소 완료: atteNo={}, empId={}, 기간={} ~ {}",
                targetAttendance.getAtteNo(),
                writer.getEmpId(),
                targetAttendance.getStartAt(),
                targetAttendance.getEndAt());
    }

    /*
    * 날짜 수정 검증
    * 원본 시작일이 과거: 시작일은 변경 불가, 종료일은 어제부터 미래까지 가능
    * 원본 시작일이 오늘, 미래: 시작은 오늘부터 미래까지 가능, 종료일은 시작일부터 미래까지 가능
    */
    private void validateModification(Attendance original, LocalDateTime newStartAt, LocalDateTime newEndAt) {
        LocalDate today = LocalDate.now();
        LocalDate originalStartDate = original.getStartAt().toLocalDate();
        LocalDate newStartDate = newStartAt.toLocalDate();
        LocalDate newEndDate = newEndAt.toLocalDate();

        // 이미 종료된 근태(종료일이 어제 이전)는 수정 불가
        if (original.getEndAt().toLocalDate().isBefore(today)) {
            throw new CustomException(ErrorCode.CANNOT_MODIFY_FINISHED_ATTENDANCE);
        }

        // 1. 원본 시작일이 과거인 경우
        if (originalStartDate.isBefore(today)) {
            // 시작일 변경 불가
            if (!newStartDate.isEqual(originalStartDate)) {
                throw new CustomException(ErrorCode.CANNOT_MODIFY_PAST_START_DATE);
            }

            // 종료일은 어제부터 미래까지 가능
            if (newEndDate.isBefore(today.minusDays(1))) {
                throw new CustomException(ErrorCode.INVALID_END_DATE_FOR_PAST_START);
            }
        }
        // 2. 원본 시작일이 오늘, 미래 경우
        else {
            // 시작일은 오늘부터 미래까지 가능
            if (newStartDate.isBefore(today)) {
                throw new CustomException(ErrorCode.START_DATE_MUST_BE_TODAY_OR_LATER);
            }
        }

        // 종료일이 시작일보다 빠를 수 없음
        if (newEndDate.isBefore(newStartDate)) {
            throw new CustomException(ErrorCode.END_DATE_BEFORE_START_DATE);
        }
    }

    /**
     * 문서 content JSON에서 attendanceInfo 파싱
     *
     * 예시 JSON:
     * {
     *   "type": "doc",
     *   "content": [...],
     *   "attendanceInfo": {
     *     "startDate": "2024-12-23",
     *     "endDate": "2024-12-25",
     *     "delegate": {
     *       "empId": "E000005"
     *     },
     *     "targetAtteNo": 123  // 수정/취소 시에만 존재
     *   }
     * }
     */
    private AttendanceInfo parseAttendanceInfo(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode attendanceNode = root.get("attendanceInfo");

            if (attendanceNode == null || attendanceNode.isNull()) {
                return null;
            }

            String startDateStr = attendanceNode.has("startDate")
                    ? attendanceNode.get("startDate").asText()
                    : null;
            String endDateStr = attendanceNode.has("endDate")
                    ? attendanceNode.get("endDate").asText()
                    : null;

            if (startDateStr == null || endDateStr == null) {
                log.warn("attendanceInfo에 startDate 또는 endDate가 없습니다.");
                return null;
            }

            // 날짜 파싱 (YYYY-MM-DD 형식)
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            // LocalDate -> LocalDateTime (00:00:00으로 변환)
            LocalDateTime startAt = startDate.atStartOfDay();
            LocalDateTime endAt = endDate.atTime(23, 59, 59);

            // 대직자 정보 파싱
            String delegateEmpId = null;
            JsonNode delegateNode = attendanceNode.get("delegate");
            if (delegateNode != null && !delegateNode.isNull()) {
                if (delegateNode.has("empId")) {
                    delegateEmpId = delegateNode.get("empId").asText();
                }
            }

            // targetAtteNo 파싱 (수정/취소 시에만 존재)
            Long targetAtteNo = null;
            if (attendanceNode.has("targetAtteNo")) {
                targetAtteNo = attendanceNode.get("targetAtteNo").asLong();
            }

            return new AttendanceInfo(startAt, endAt, delegateEmpId, targetAtteNo);

        } catch (Exception e) {
            log.error("attendanceInfo 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    // 근태 식별자로 근태 조회
    @Transactional(readOnly = true)
    public ResAttendanceDto getAttendanceByAtteNo(String comId, String empId, Long atteNo) {
        Attendance attendance = attendanceRepository.findById(atteNo)
                .orElseThrow(() -> new CustomException(ErrorCode.ATTENDANCE_NOT_FOUND));

        // 회사 확인
        if (!attendance.getEmployee().getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.COMPANY_MISMATCH);
        }

        return ResAttendanceDto.toDto(attendance);
    }

    // 내 휴가 정보 조회
    @Transactional(readOnly = true)
    public List<ResAttendanceDto> getMyVacations(String comId, String empId) {
        return attendanceRepository.findByEmployee_EmpIdAndCompany_ComIdAndTypeAndIsDeletedFalseOrderByStartAtDesc(empId, comId, AtteType.V)
                .stream().map(ResAttendanceDto::toDto).toList();
    }

    // 내 출장 정보 조회
    @Transactional(readOnly = true)
    public List<ResAttendanceDto> getMyBusinessTrips(String comId, String empId) {
        return attendanceRepository.findByEmployee_EmpIdAndCompany_ComIdAndTypeAndIsDeletedFalseOrderByStartAtDesc(empId, comId, AtteType.B)
                .stream().map(ResAttendanceDto::toDto).toList();
    }

    // 회사의 전체 근태 조회
    @Transactional(readOnly = true)
    public Page<ResAttendanceDto> getAllAttendances(String comId, Pageable pageable) {
        return attendanceRepository.findByCompany_ComIdAndIsDeletedFalseOrderByStartAtDesc(comId, pageable)
                .map(ResAttendanceDto::toDto);
    }

    // 수정가능한 내 휴가, 출장 조회
    @Transactional(readOnly = true)
    public List<ResAttendanceDto> getMyModifiableAttendances(String comId, String myEmpId, String atteType){


        if("V".equals(atteType) || "B".equals(atteType)) {
            AtteType type = AtteType.valueOf(atteType);
            return attendanceRepository.findModifiableAttendances(comId, myEmpId, LocalDateTime.now(), type)
                    .stream().map(ResAttendanceDto::toDto).toList();
        }
        else{
            throw new CustomException(ErrorCode.BAD_ATTENDANCE_REQUEST);
        }
    }

    // 취소가능한 내 휴가, 출장 조회
    @Transactional(readOnly = true)
    public List<ResAttendanceDto> getMyCancelableAttendances(String comId, String myEmpId, String atteType){
        if("V".equals(atteType) || "B".equals(atteType)) {
            AtteType type = AtteType.valueOf(atteType);
            return attendanceRepository.findCancelableAttendances(comId, myEmpId, LocalDateTime.now(), type)
                .stream().map(ResAttendanceDto::toDto).toList();
        }
        else{
            throw new CustomException(ErrorCode.BAD_ATTENDANCE_REQUEST);
        }
    }


    // 상호 대직 및 데드락 방지 통합 검증
//    private void validateAttendanceIntegrity(Employee writer, LocalDateTime start, LocalDateTime end) {
//        // 내가 누군가의 대직자로 활동해야 하는 기간과 겹치는지 체크 (역방향 체크)
//        if (attendanceRepository.existsByDelegateAndDateOverlap(writer, start, end)) {
//            throw new CustomException(ErrorCode.CANNOT_LEAVE_WHILE_ACTING_AS_DELEGATE);
//        }
//    }

    // 나를 대직자로 설정한 사람들의 근태 목록 조회
    @Transactional(readOnly = true)
    public List<ResAttendanceDto> getPeriodsWhereIAmDelegate(String myEmpId) {
        Employee me = employeeRepository.findByEmpId(myEmpId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        // 나를 대직자로 지정한 모든 활성 근태 기록 조회
        return attendanceRepository.findAllByDelegateAndIsDeletedFalse(me)
                .stream()
                .map(ResAttendanceDto::toDto)
                .toList();
    }

    // 내 근태 중복 여부 체크
    @Transactional(readOnly = true)
    public List<ResAttendanceDto> getMyAttendanceOverlapList(String comId, String empId, LocalDateTime start, LocalDateTime end, Long excludeAtteNo) {
        Employee employee = employeeRepository.findByEmpId(empId).orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));

        List<Attendance> overlaps;
        if (excludeAtteNo != null) {
            // 수정 시 본인 제외 쿼리 (이미 Repository에 있는 exists... 로직을 findAll...로 변경한 쿼리 필요)
            overlaps = attendanceRepository.findAllByEmployeeAndDateOverlapExcludeSelf(employee, start, end, excludeAtteNo);
        } else {
            // 신규 신청 시 쿼리
            overlaps = attendanceRepository.findAllByEmployeeAndDateOverlap(employee, start, end);
        }
        return overlaps.stream().map(ResAttendanceDto::toDto).toList();
    }


    // 대직자에게 알림 전송 (지정/해제)
    private void notifyDelegate(Employee writer, Employee delegate, Document document, String title, String messageSuffix) {
        if (delegate == null) return;

        notificationService.sendNotification(
                delegate,
                NotificationType.OTHER, // 또는 별도의 ATTENDANCE 타입이 있다면 사용
                title,
                writer.getEmpName() + " " + writer.getPositions().getPosName() + messageSuffix,
                "/documents/" + document.getDocNo() + "?status=FINALIZED" // 최종 승인 문서 상세로 이동
        );
    }
}