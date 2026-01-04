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
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 1. 대상 데이터 조회 (휴가/출장 모두 포함, 삭제된 것 제외)
        List<Attendance> attendances = attendanceRepository.findByStartAtIsTodayOrEndAtIsYesterdayAndIsDeletedFalse(today, yesterday);

        // [Step 1] 어제 근태가 끝난 사원들 복귀 처리
        for (Attendance attendance : attendances) {
            if (attendance.getEndAt().toLocalDate().isEqual(yesterday)) {
                Employee onLeave = attendance.getEmployee();
                Employee delegate = attendance.getDelegate();

                // 결재라인에서 대직자 제거
                removeDelegateFromApprovalLines(onLeave, delegate);
                // 상태값 원복
                onLeave.updateDelegate(null);
                onLeave.updateAtteStatus("C");

                log.info("근태 종료 처리: 사원={}, 기존대직자={}", onLeave.getEmpId(),
                        delegate != null ? delegate.getEmpId() : "없음");
            }
        }

        // [Step 2] 오늘 근태가 시작되는 사원들의 "상태값" 먼저 모두 변경 (체인 로직 준비)
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

        // [Step 3] 상태 업데이트가 완료된 후, 휴가자(V)에 대한 결재라인 대직자 투입 실행
        for (Attendance attendance : attendances) {
            if (attendance.getStartAt().toLocalDate().isEqual(today) && attendance.getType() == AtteType.V) {
                if (attendance.getDelegate() != null) {
                    addDelegateToApprovalLines(attendance.getEmployee(), attendance.getDelegate());
                    log.info("대직자 결재 권한 투입: 휴가자={}, 대직자={}",
                            attendance.getEmployee().getEmpId(), attendance.getDelegate().getEmpId());
                }
            }
        }
    }
//    public void updateDelegatesSchedule() {
//
//        // 1. 근태타입이 휴가(V)이면서 endAt이 어제이면 사원테이블의 delegate를 null로 변경
//        // 2. 근태타입이 휴가(V)이면서 startAt이 오늘이면 근태테이블의 대직자를 사원테이블의 delegate에 반영
//        // 1번 -> 2번 순서대로 해야 대직자를 넣고 null로 처리하는 참사가 발생하지 않음
//
//        // 휴가(V)에서 endAt(종료일)이 어제인것 or startAt(시작일)이 오늘인 근태정보 조회
//        List<Attendance> attendances = attendanceRepository.findByStartAtIsTodayOrEndAtIsYesterdayAndIsDeletedFalse(
//                LocalDate.now(),
//                LocalDate.now().minusDays(1)
//        );
//
//        LocalDate today = LocalDate.now();
//        LocalDate yesterday = today.minusDays(1);
//
//        // 어제 휴가가 끝난 사원들 대직자 해제 (null 처리), 결재라인에서 대직자 제거
//        for (Attendance attendance : attendances) {
//            LocalDate endAt = attendance.getEndAt().toLocalDate();
//            Employee onLeave = attendance.getEmployee();
//            Employee delegate = attendance.getDelegate();
//
//            if (endAt != null && endAt.isEqual(yesterday) && onLeave != null) {
//                // 결재라인에서 대직자 삭제 (2단계 방식)
//                removeDelegateFromApprovalLines(onLeave, delegate);
//
//                // 휴가자의 대직자 제거
//                onLeave.updateDelegate(null);
//
//                onLeave.updateAtteStatus("C");
//
//                if (delegate != null) {
//                    log.info("휴가 종료로 인한 대직자 해제: 휴가자={}, 기존대직자={}",
//                            onLeave.getEmpId(), delegate.getEmpId());
//                } else {
//                    log.info("휴가 종료로 인한 대직자 해제: 휴가자={}, 기존대직자=없음",
//                            onLeave.getEmpId());
//                }
//            }
//        }
//
//        // 오늘 휴가가 시작되는 사원들 대직자 설정
//        for (Attendance attendance : attendances) {
//            LocalDate startAt = attendance.getStartAt().toLocalDate();
//            Employee onLeave = attendance.getEmployee();
//            Employee delegate = attendance.getDelegate();
//
//            if (startAt != null && startAt.isEqual(today) && onLeave != null) {
//
//                // 휴가(V)나 출장(B) 타입에 맞춰 무조건 수행
//                String atteTypeStr = (attendance.getType() == AtteType.V) ? "V" : "B";
//
//                // 이미 서비스에서 즉시 처리되어 상태가 반영된 경우 중복 처리 방지
//                if (!onLeave.getAtte().equals(atteTypeStr)) {
//                    onLeave.updateAtteStatus(atteTypeStr);
//                    log.info("근태 시작 상태 변경: 사원={}, 상태={}", onLeave.getEmpId(), atteTypeStr);
//                }
//
//                // 대직자 및 결재라인 투입: 휴가(V)이면서 대직자가 있는 경우만 수행
//                if (attendance.getType() == AtteType.V && delegate != null) {
//                    // 이미 설정된 대직자가 아니라면 업데이트
//                    if (onLeave.getDelegate() == null || !onLeave.getDelegate().getEmpId().equals(delegate.getEmpId())) {
//                        onLeave.updateDelegate(delegate);
//                        addDelegateToApprovalLines(onLeave, delegate);
//                        log.info("대직자 투입 완료: 사원={}, 대직자={}", onLeave.getEmpId(), delegate.getEmpId());
//                    }
//                }
//            }
//        }
//    }

    // 결재라인에 대직자 추가
//    public void addDelegateToApprovalLines(Employee onLeave, Employee firstDelegate) {
//        DocStat[] docStats = {DocStat.AW, DocStat.US};
//        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};
//
//        List<ApprovalLine> targetLines = approvalLineRepository.findApprovalLinesForVacation(
//                onLeave, docStats, apprStats
//        );
//
//        for (ApprovalLine originalLine : targetLines) {
//            Employee currentTarget = onLeave;
//            Employee currentDelegate = firstDelegate;
//
//            // 투입되는 대직자부터 시작해서 체인이 끝날 때까지 반복
//            while (currentDelegate != null) {
//                // 중복 체크 (이미 이 target을 위해 이 대직자가 들어와 있는지)
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
//                }
//
//                // 만약 새로 투입된 대직자도 휴가 중이라면, 그 사람의 대직자도 이 seq에 투입해야 함
//                if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
//                    currentTarget = currentDelegate;
//                    currentDelegate = currentDelegate.getDelegate();
//                } else {
//                    break; // 출근 중인 대직자를 찾았으므로 종료
//                }
//            }
//        }
//    }

    public void addDelegateToApprovalLines(Employee onLeave, Employee firstDelegate) {
        DocStat[] docStats = {DocStat.AW, DocStat.US};
        ApprStat[] apprStats = {ApprStat.I, ApprStat.W};

        // 휴가자가 결재자로 지정된 진행 중인 문서들 조회
        List<ApprovalLine> targetLines = approvalLineRepository.findApprovalLinesForVacation(
                onLeave, docStats, apprStats
        );

        for (ApprovalLine originalLine : targetLines) {
            // 해당 문서의 실제 작성자 가져오기
            Employee docWriter = originalLine.getDocument().getWriter();

            Employee currentTarget = onLeave;
            Employee currentDelegate = firstDelegate;

            // 대직자 체인을 따라가되, 각 단계에서 작성자 본인 여부 체크
            while (currentDelegate != null) {

                // ✅ 핵심 조건: 대직자가 이 문서의 작성자라면 결재라인에 추가하지 않음
                if (currentDelegate.getEmpId().equals(docWriter.getEmpId())) {
                    log.info("대직자가 문서 작성자 본인이므로 결재라인 추가 제외: 문서={}, 사번={}",
                            originalLine.getDocument().getDocNo(), currentDelegate.getEmpId());

                    // 만약 현재 대직자가 작성자라서 스킵했는데, 이 사람도 휴가 중이라면?
                    // 다음 대직자 체인을 확인하여 '작성자가 아닌 다음 대직자'를 찾습니다.
                    if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
                        currentTarget = currentDelegate;
                        currentDelegate = currentDelegate.getDelegate();
                        continue; // 다음 체인 인원으로 다시 체크 루프 실행
                    } else {
                        break; // 더 이상 대행할 인원이 없으면 종료
                    }
                }

                // 중복 체크 로직
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

                // 만약 새로 투입된 대직자도 휴가 중이라면 그 사람의 대직자도 이 순번(Seq)에 투입
                if ("V".equals(currentDelegate.getAtte()) && currentDelegate.getDelegate() != null) {
                    currentTarget = currentDelegate;
                    currentDelegate = currentDelegate.getDelegate();
                } else {
                    break; // 대행 가능한 사람을 찾았으므로 종료
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