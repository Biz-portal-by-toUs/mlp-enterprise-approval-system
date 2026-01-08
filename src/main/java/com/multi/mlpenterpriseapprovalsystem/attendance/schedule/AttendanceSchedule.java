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
 * Please explain the class!!!
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

    @Scheduled(cron = "0 1 0 * * *")
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

                // ✅ [알림] 대직 종료 알림
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
//                if (attendance.getDelegate() != null) {
//                    addDelegateToApprovalLines(attendance.getEmployee(), attendance.getDelegate());
//                }

                Employee onLeave = attendance.getEmployee();
                Employee delegate = attendance.getDelegate();

                if (delegate != null) {
                    addDelegateToApprovalLines(onLeave, delegate);

                    // ✅ [알림] 오늘부터 대직 업무 시작 알림
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

            // ✅ 순환 참조 방지를 위한 Set 추가
            Set<String> visitedEmpIds = new HashSet<>();
            // 원 결재자(onLeave)를 미리 방문 목록에 추가하여 자신에게 돌아오는 것 방지
            visitedEmpIds.add(onLeave.getEmpId());

            while (currentDelegate != null) {
                // ✅ 순환 참조 체크: 이미 처리한 대직자라면 체인 중단
                if (visitedEmpIds.contains(currentDelegate.getEmpId())) {
                    log.error("대직 체인 순환 참조 감지 및 차단: 문서={}, 사번={}",
                            originalLine.getDocument().getDocNo(), currentDelegate.getEmpId());
                    break;
                }

                // 작성자 본인 체크
//                if (currentDelegate.getEmpId().equals(docWriter.getEmpId())) {
//                    log.info("대직자가 문서 작성자 본인이므로 결재라인 추가 제외: 문서={}, 사번={}",
//                            originalLine.getDocument().getDocNo(), currentDelegate.getEmpId());
//
//                    if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
//                        visitedEmpIds.add(currentDelegate.getEmpId()); // 방문 기록
//                        currentTarget = currentDelegate;
//                        currentDelegate = currentDelegate.getDelegate();
//                        continue;
//                    } else {
//                        break;
//                    }
//                }

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

//    @Scheduled(cron = "0 1 0 * * *")
//    public void updateEmployeeDelegates() {
//        // 1. 오늘 시작하는 휴가자 조회 및 사원 테이블 대직자 갱신, 결재라인에 대직자 추가
//        // 2. 어제 휴가가 끝난 사원의 대직자 컬럼 초기화(NULL), 결재라인에 대직자 삭제
//        //    주의: 대직자 삭제 시 결재상태가 I(결재중), W(결재대기중)인 결재라인만 삭제. 승인이나 반려한 결재라인은 삭제하면 안됨.
//
//        updateDelegatesSchedule();
//    }
//
//
//    // 근태테이블에서 휴가정보를 조회하여 사원테이블의 대직자 갱신
//    public void updateDelegatesSchedule() {
//        LocalDate today = LocalDate.now();
//        LocalDate yesterday = today.minusDays(1);
//
//        // 1. 대상 데이터 조회 (휴가/출장 모두 포함, 삭제된 것 제외)
//        List<Attendance> attendances = attendanceRepository.findByStartAtIsTodayOrEndAtIsYesterdayAndIsDeletedFalse(today, yesterday);
//
//        // [Step 1] 어제 근태가 끝난 사원들 복귀 처리
//        for (Attendance attendance : attendances) {
//            if (attendance.getEndAt().toLocalDate().isEqual(yesterday)) {
//                Employee onLeave = attendance.getEmployee();
//                Employee delegate = attendance.getDelegate();
//
//                // 결재라인에서 대직자 제거
//                removeDelegateFromApprovalLines(onLeave, delegate);
//                // 상태값 원복
//                onLeave.updateDelegate(null);
//                onLeave.updateAtteStatus("C");
//
//                log.info("근태 종료 처리: 사원={}, 기존대직자={}", onLeave.getEmpId(),
//                        delegate != null ? delegate.getEmpId() : "없음");
//            }
//        }
//
//        // [Step 2] 오늘 근태가 시작되는 사원들의 "상태값" 먼저 모두 변경 (체인 로직 준비)
//        for (Attendance attendance : attendances) {
//            if (attendance.getStartAt().toLocalDate().isEqual(today)) {
//                Employee onLeave = attendance.getEmployee();
//                String atteTypeStr = (attendance.getType() == AtteType.V) ? "V" : "B";
//
//                onLeave.updateAtteStatus(atteTypeStr);
//                if (attendance.getType() == AtteType.V) {
//                    onLeave.updateDelegate(attendance.getDelegate());
//                }
//            }
//        }
//
//        // [Step 3] 상태 업데이트가 완료된 후, 휴가자(V)에 대한 결재라인 대직자 투입 실행
//        for (Attendance attendance : attendances) {
//            if (attendance.getStartAt().toLocalDate().isEqual(today) && attendance.getType() == AtteType.V) {
//                if (attendance.getDelegate() != null) {
//                    addDelegateToApprovalLines(attendance.getEmployee(), attendance.getDelegate());
//                    log.info("대직자 결재 권한 투입: 휴가자={}, 대직자={}",
//                            attendance.getEmployee().getEmpId(), attendance.getDelegate().getEmpId());
//                }
//            }
//        }
//    }
//
//    public void addDelegateToApprovalLines(Employee onLeave, Employee firstDelegate) {
//        DocStat[] docStats = {DocStat.AW, DocStat.US};
//        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};
//
//        // 휴가자가 결재자로 지정된 진행 중인 문서들 조회
//        List<ApprovalLine> targetLines = approvalLineRepository.findApprovalLinesForVacation(
//                onLeave, docStats, apprStats
//        );
//
//        for (ApprovalLine originalLine : targetLines) {
//            // 해당 문서의 실제 작성자 가져오기
//            Employee docWriter = originalLine.getDocument().getWriter();
//
//            Employee currentTarget = onLeave;
//            Employee currentDelegate = firstDelegate;
//
//            // 대직자 체인을 따라가되, 각 단계에서 작성자 본인 여부 체크
//            while (currentDelegate != null) {
//
//                // ✅ 핵심 조건: 대직자가 이 문서의 작성자라면 결재라인에 추가하지 않음
//                if (currentDelegate.getEmpId().equals(docWriter.getEmpId())) {
//                    log.info("대직자가 문서 작성자 본인이므로 결재라인 추가 제외: 문서={}, 사번={}",
//                            originalLine.getDocument().getDocNo(), currentDelegate.getEmpId());
//
//                    // 만약 현재 대직자가 작성자라서 스킵했는데, 이 사람도 휴가 중이라면?
//                    // 다음 대직자 체인을 확인하여 '작성자가 아닌 다음 대직자'를 찾습니다.
//                    if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
//                        currentTarget = currentDelegate;
//                        currentDelegate = currentDelegate.getDelegate();
//                        continue; // 다음 체인 인원으로 다시 체크 루프 실행
//                    } else {
//                        break; // 더 이상 대행할 인원이 없으면 종료
//                    }
//                }
//
//                // 중복 체크 로직
//                boolean delegateExists = approvalLineRepository.existsByDocumentAndSeqAndApproverAndIsDelegateAndTargetApprover(
//                        originalLine.getDocument(), originalLine.getSeq(), currentDelegate, true, currentTarget
//                );
//
//                if (!delegateExists) {
//                    ApprovalLine delegateLine = ApprovalLine.toEntity(
//                            originalLine.getDocument(), currentDelegate, originalLine.getCompany(),
//                            originalLine.getSeq(), originalLine.getApprStat(), true, currentTarget
//                    );
//                    approvalLineRepository.save(delegateLine);
//                    log.info("결재라인 대직자 투입 완료: 문서={}, 대직자={}",
//                            originalLine.getDocument().getDocNo(), currentDelegate.getEmpId());
//                }
//
//                // 만약 새로 투입된 대직자도 휴가 중이라면 그 사람의 대직자도 이 순번(Seq)에 투입
//                if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
//                    currentTarget = currentDelegate;
//                    currentDelegate = currentDelegate.getDelegate();
//                } else {
//                    break; // 대행 가능한 사람을 찾았으므로 종료
//                }
//            }
//        }
//    }
//
//
//    // 결재라인에서 대직자 제거 (2단계 방식)
//    public void removeDelegateFromApprovalLines(Employee onLeave, Employee delegate) {
//        DocStat[] docStats = {DocStat.AW, DocStat.US};
//        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};
//
//        // Step 1: 삭제할 결재라인 ID 조회
//        List<Long> apprlNosToDelete = approvalLineRepository.findDelegateApprovalLineIdsToDelete(
//                onLeave, delegate, docStats, apprStats
//        );
//
//        if (apprlNosToDelete.isEmpty()) {
//            log.info("제거할 대직자 결재라인 없음: 휴가자={}, 대직자={}",
//                    onLeave.getEmpId(), delegate.getEmpId());
//            return;
//        }
//
//        // Step 2: ID로 삭제
//        approvalLineRepository.deleteByApprlNoIn(apprlNosToDelete);
//
//        log.info("결재라인에서 대직자 제거 완료: 휴가자={}, 대직자={}, 제거된 개수={}",
//                onLeave.getEmpId(), delegate.getEmpId(), apprlNosToDelete.size());
//    }
}