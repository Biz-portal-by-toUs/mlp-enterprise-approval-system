package com.multi.mlpenterpriseapprovalsystem.attendance.schedule;

import com.multi.mlpenterpriseapprovalsystem.attendance.domain.Attendance;
import com.multi.mlpenterpriseapprovalsystem.attendance.enums.AtteType;
import com.multi.mlpenterpriseapprovalsystem.attendance.repository.AttendanceRepository;
import com.multi.mlpenterpriseapprovalsystem.document.domain.ApprovalLine;
import com.multi.mlpenterpriseapprovalsystem.document.enums.ApprStat;
import com.multi.mlpenterpriseapprovalsystem.document.enums.DocStat;
import com.multi.mlpenterpriseapprovalsystem.document.repository.ApprovalLineRepository;
import com.multi.mlpenterpriseapprovalsystem.document.service.ApprovalLineService;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 근태 스케쥴러
 * 오늘이 근태 시작일이면 근태시작 처리
 * 어제가 근태 종료일이면 근태 복귀 처리
 *
 * @author : 이지헌
 * @filename : AttendanceSchedule
 * @since : 25. 12. 30. 화요일
 */
@Transactional
@Slf4j
@RequiredArgsConstructor
@Service
public class AttendanceSchedule {

    private final ApprovalLineService approvalLineService;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final ApprovalLineRepository approvalLineRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 3 0 * * *", zone = "Asia/Seoul")
    public void updateEmployeeDelegates() {
        updateDelegatesSchedule();
    }

    public void updateDelegatesSchedule() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<Attendance> attendances = attendanceRepository.findByStartAtIsTodayOrEndAtIsYesterdayAndIsDeletedFalse(today, yesterday);

        // [Step 1] 어제 근태가 끝난 사원들 복귀 처리
        for (Attendance attendance : attendances) {
            if (attendance.getEndAt().toLocalDate().isEqual(yesterday)) {
                Employee onLeave = attendance.getEmployee();
                Employee delegate = attendance.getDelegate();

                removeDelegateFromApprovalLines(onLeave, delegate);
                onLeave.updateDelegate(null);
                onLeave.updateAtteStatus("C");

                // [알림] 대직 종료 알림
                if (delegate != null) {
                    notificationService.sendNotification(
                            delegate,
                            NotificationType.OTHER,
                            "[대직 업무 종료]",
                            onLeave.getEmpName() + "님의 복귀로 대직 업무가 종료되었습니다.",
                            "/documents/me?status=PROCESSED" // 내가 처리한 문서함으로 이동
                    );
                }

                log.info("근태 종료 처리: 사원={}, 기존대직자={}", onLeave.getEmpId(),
                        delegate != null ? delegate.getEmpId() : "없음");
            }
        }

        // [Step 2] 오늘 근태가 시작되는 사원들의 "상태값" 먼저 모두 변경
        for (Attendance attendance : attendances) {
            if (attendance.getStartAt().toLocalDate().isEqual(today)) {
                Employee onLeave = attendance.getEmployee();
                String atteTypeStr = (attendance.getType() == AtteType.V) ? "V" : "B";

                onLeave.updateAtteStatus(atteTypeStr);
                if (attendance.getType() == AtteType.V) {
                    onLeave.updateDelegate(attendance.getDelegate());
                }
            }
        }

        // [Step 3] 결재라인 대직자 투입 실행
        for (Attendance attendance : attendances) {
            if (attendance.getStartAt().toLocalDate().isEqual(today) && attendance.getType() == AtteType.V) {

                Employee onLeave = attendance.getEmployee();
                Employee delegate = attendance.getDelegate();

                if (delegate != null) {
                    addDelegateToApprovalLines(onLeave, delegate);

                    // [알림] 오늘부터 대직 업무 시작 알림
                    notificationService.sendNotification(
                            delegate,
                            NotificationType.OTHER,
                            "[대직 업무 시작]",
                            "오늘부터 " + onLeave.getEmpName() + "님의 대직 업무가 시작됩니다. 결재할 문서를 확인해주세요.",
                            "/documents/me?status=AWAITING" // 결재할 문서함으로 이동
                    );

                    log.info("대직 시작 알림 발송: 대직자={}", delegate.getEmpId());
                }
            }
        }
    }

    public void addDelegateToApprovalLines(Employee onLeave, Employee firstDelegate) {
        DocStat[] docStats = {DocStat.AW, DocStat.US};
        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};

        List<ApprovalLine> targetLines = approvalLineRepository.findApprovalLinesForVacation(
                onLeave, docStats, apprStats
        );

        for (ApprovalLine originalLine : targetLines) {
            Employee docWriter = originalLine.getDocument().getWriter();
            Employee currentTarget = onLeave;
            Employee currentDelegate = firstDelegate;

            // 순환 참조 방지를 위한 Set 추가
            Set<String> visitedEmpIds = new HashSet<>();
            // 원 결재자(onLeave)를 미리 방문 목록에 추가하여 자신에게 돌아오는 것 방지
            visitedEmpIds.add(onLeave.getEmpId());

            while (currentDelegate != null) {
                // 순환 참조 체크: 이미 처리한 대직자라면 체인 중단
                if (visitedEmpIds.contains(currentDelegate.getEmpId())) {
                    log.error("대직 체인 순환 참조 감지 및 차단: 문서={}, 사번={}",
                            originalLine.getDocument().getDocNo(), currentDelegate.getEmpId());
                    break;
                }

                // 중복 등록 방지 체크
                boolean delegateExists = approvalLineRepository.existsByDocumentAndSeqAndApproverAndIsDelegateAndTargetApprover(
                        originalLine.getDocument(), originalLine.getSeq(), currentDelegate, true, currentTarget
                );

                if (!delegateExists) {
                    ApprovalLine delegateLine = ApprovalLine.toEntity(
                            originalLine.getDocument(), currentDelegate, originalLine.getCompany(),
                            originalLine.getSeq(), originalLine.getApprStat(), true, currentTarget
                    );
                    approvalLineRepository.save(delegateLine);
                    log.info("결재라인 대직자 투입 완료: 문서={}, 대직자={}",
                            originalLine.getDocument().getDocNo(), currentDelegate.getEmpId());
                }

                // 현재 대직자를 방문 목록에 추가
                visitedEmpIds.add(currentDelegate.getEmpId());

                // 연쇄 대직자 추적 (현재 대직자도 휴가 중인 경우)
                if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
                    currentTarget = currentDelegate;
                    currentDelegate = currentDelegate.getDelegate();
                } else {
                    break;
                }
            }
        }
    }

    public void removeDelegateFromApprovalLines(Employee onLeave, Employee delegate) {
        DocStat[] docStats = {DocStat.AW, DocStat.US};
        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};

        List<Long> apprlNosToDelete = approvalLineRepository.findDelegateApprovalLineIdsToDelete(
                onLeave, delegate, docStats, apprStats
        );

        if (!apprlNosToDelete.isEmpty()) {
            approvalLineRepository.deleteByApprlNoIn(apprlNosToDelete);
            log.info("결재라인에서 대직자 제거 완료: 휴가자={}, 제거 개수={}",
                    onLeave.getEmpId(), apprlNosToDelete.size());
        }
    }
}