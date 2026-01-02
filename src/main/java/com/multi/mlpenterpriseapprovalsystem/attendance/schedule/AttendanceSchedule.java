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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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

    @Scheduled(cron = "0 1 0 * * *")
    public void updateEmployeeDelegates() {
        // 1. 오늘 시작하는 휴가자 조회 및 사원 테이블 대직자 갱신, 결재라인에 대직자 추가
        // 2. 어제 휴가가 끝난 사원의 대직자 컬럼 초기화(NULL), 결재라인에 대직자 삭제
        //    주의: 대직자 삭제 시 결재상태가 I(결재중), W(결재대기중)인 결재라인만 삭제. 승인이나 반려한 결재라인은 삭제하면 안됨.

        updateDelegatesSchedule();
    }


    // 근태테이블에서 휴가정보를 조회하여 사원테이블의 대직자 갱신
    public void updateDelegatesSchedule() {

        // 1. 근태타입이 휴가(V)이면서 endAt이 어제이면 사원테이블의 delegate를 null로 변경
        // 2. 근태타입이 휴가(V)이면서 startAt이 오늘이면 근태테이블의 대직자를 사원테이블의 delegate에 반영
        // 1번 -> 2번 순서대로 해야 대직자를 넣고 null로 처리하는 참사가 발생하지 않음

        // 휴가(V)에서 endAt(종료일)이 어제인것 or startAt(시작일)이 오늘인 근태정보 조회
        List<Attendance> attendances = attendanceRepository.findByStartAtIsTodayOrEndAtIsYesterday(
                LocalDate.now(),
                LocalDate.now().minusDays(1)
        );

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 어제 휴가가 끝난 사원들 대직자 해제 (null 처리), 결재라인에서 대직자 제거
        for (Attendance attendance : attendances) {
            LocalDate endAt = attendance.getEndAt().toLocalDate();
            Employee onLeave = attendance.getEmployee();
            Employee delegate = attendance.getDelegate();

            if (endAt != null && endAt.isEqual(yesterday) && onLeave != null) {
                // 결재라인에서 대직자 삭제 (2단계 방식)
                removeDelegateFromApprovalLines(onLeave, delegate);

                // 휴가자의 대직자 제거
                onLeave.updateDelegate(null);

                onLeave.updateAtteStatus("C");

                if (delegate != null) {
                    log.info("휴가 종료로 인한 대직자 해제: 휴가자={}, 기존대직자={}",
                            onLeave.getEmpId(), delegate.getEmpId());
                } else {
                    log.info("휴가 종료로 인한 대직자 해제: 휴가자={}, 기존대직자=없음",
                            onLeave.getEmpId());
                }
            }
        }

        // 오늘 휴가가 시작되는 사원들 대직자 설정
        for (Attendance attendance : attendances) {
            LocalDate startAt = attendance.getStartAt().toLocalDate();
            Employee onLeave = attendance.getEmployee();
            Employee delegate = attendance.getDelegate();

            if (startAt != null && startAt.isEqual(today) && onLeave != null) {

                // 휴가(V)나 출장(B) 타입에 맞춰 무조건 수행
                String atteTypeStr = (attendance.getType() == AtteType.V) ? "V" : "B";

                // 이미 서비스에서 즉시 처리되어 상태가 반영된 경우 중복 처리 방지
                if (!onLeave.getAtte().equals(atteTypeStr)) {
                    onLeave.updateAtteStatus(atteTypeStr);
                    log.info("근태 시작 상태 변경: 사원={}, 상태={}", onLeave.getEmpId(), atteTypeStr);
                }

                // 대직자 및 결재라인 투입: 휴가(V)이면서 대직자가 있는 경우만 수행
                if (attendance.getType() == AtteType.V && delegate != null) {
                    // 이미 설정된 대직자가 아니라면 업데이트
                    if (onLeave.getDelegate() == null || !onLeave.getDelegate().getEmpId().equals(delegate.getEmpId())) {
                        onLeave.updateDelegate(delegate);
                        addDelegateToApprovalLines(onLeave, delegate);
                        log.info("대직자 투입 완료: 사원={}, 대직자={}", onLeave.getEmpId(), delegate.getEmpId());
                    }
                }

//                // 이미 대직자가 설정되어 있으면 스킵 (즉시 처리된 경우)
//                if (onLeave.getDelegate() != null && onLeave.getDelegate().getEmpId().equals(delegate.getEmpId())) {
//                    log.info("이미 대직자 설정됨 (즉시처리): 휴가자={}", onLeave.getEmpId());
//                    continue;
//                }
//
//                // 사원 테이블 대직자 설정
//                onLeave.updateDelegate(delegate);
//
//                // 결재라인에 대직자 추가
//                addDelegateToApprovalLines(onLeave, delegate);
//
//                log.info("휴가 시작으로 인한 대직자 설정: 휴가자={}, 대직자={}", onLeave.getEmpId(), delegate.getEmpId());
            }
        }
    }

    // 결재라인에 대직자 추가
//    public void addDelegateToApprovalLines(Employee onLeave, Employee delegate) {
//
//        DocStat[] docStats = {DocStat.AW, DocStat.US};
//        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};
//
//        List<ApprovalLine> targetLines = approvalLineRepository.findApprovalLinesForVacation(
//                onLeave, docStats, apprStats
//        );
//
//        for (ApprovalLine originalLine : targetLines) {
//            // 이미 대직자가 추가되어 있는지 확인
//            boolean delegateExists = approvalLineRepository.existsByDocumentAndSeqAndApproverAndIsDelegateAndTargetApprover(
//                    originalLine.getDocument(),
//                    originalLine.getSeq(),
//                    delegate,
//                    true,
//                    onLeave
//            );
//
//            if (delegateExists) {
//                log.info("대직자 결재라인 이미 존재: 문서={}, seq={}, 대직자={}",
//                        originalLine.getDocument().getDocNo(),
//                        originalLine.getSeq(),
//                        delegate.getEmpId()
//                );
//                continue;  // 이미 존재하면 스킵
//            }
//
//            ApprovalLine delegateLine = ApprovalLine.toEntity(
//                    originalLine.getDocument(),
//                    delegate,
//                    originalLine.getCompany(),
//                    originalLine.getSeq(),
//                    originalLine.getApprStat(),
//                    true,
//                    onLeave
//            );
//            approvalLineRepository.save(delegateLine);
//            log.info("결재라인 대직자 추가: 문서={}, seq={}, 휴가자={}, 대직자={}",
//                    originalLine.getDocument().getDocNo(),
//                    originalLine.getSeq(),
//                    onLeave.getEmpId(),
//                    delegate.getEmpId()
//            );
//        }
//    }

    public void addDelegateToApprovalLines(Employee onLeave, Employee firstDelegate) {
        DocStat[] docStats = {DocStat.AW, DocStat.US};
        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};

        List<ApprovalLine> targetLines = approvalLineRepository.findApprovalLinesForVacation(
                onLeave, docStats, apprStats
        );

        for (ApprovalLine originalLine : targetLines) {
            Employee currentTarget = onLeave;
            Employee currentDelegate = firstDelegate;

            // 투입되는 대직자부터 시작해서 체인이 끝날 때까지 반복
            while (currentDelegate != null) {
                // 중복 체크 (이미 이 target을 위해 이 대직자가 들어와 있는지)
                boolean delegateExists = approvalLineRepository.existsByDocumentAndSeqAndApproverAndIsDelegateAndTargetApprover(
                        originalLine.getDocument(), originalLine.getSeq(), currentDelegate, true, currentTarget
                );

                if (!delegateExists) {
                    ApprovalLine delegateLine = ApprovalLine.toEntity(
                            originalLine.getDocument(), currentDelegate, originalLine.getCompany(),
                            originalLine.getSeq(), originalLine.getApprStat(), true, currentTarget
                    );
                    approvalLineRepository.save(delegateLine);
                }

                // 만약 새로 투입된 대직자도 휴가 중이라면, 그 사람의 대직자도 이 seq에 투입해야 함
                if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
                    currentTarget = currentDelegate;
                    currentDelegate = currentDelegate.getDelegate();
                } else {
                    break; // 출근 중인 대직자를 찾았으므로 종료
                }
            }
        }
    }


    // 결재라인에서 대직자 제거 (2단계 방식)
    public void removeDelegateFromApprovalLines(Employee onLeave, Employee delegate) {
        DocStat[] docStats = {DocStat.AW, DocStat.US};
        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};

        // Step 1: 삭제할 결재라인 ID 조회
        List<Long> apprlNosToDelete = approvalLineRepository.findDelegateApprovalLineIdsToDelete(
                onLeave, delegate, docStats, apprStats
        );

        if (apprlNosToDelete.isEmpty()) {
            log.info("제거할 대직자 결재라인 없음: 휴가자={}, 대직자={}",
                    onLeave.getEmpId(), delegate.getEmpId());
            return;
        }

        // Step 2: ID로 삭제
        approvalLineRepository.deleteByApprlNoIn(apprlNosToDelete);

        log.info("결재라인에서 대직자 제거 완료: 휴가자={}, 대직자={}, 제거된 개수={}",
                onLeave.getEmpId(), delegate.getEmpId(), apprlNosToDelete.size());
    }
}