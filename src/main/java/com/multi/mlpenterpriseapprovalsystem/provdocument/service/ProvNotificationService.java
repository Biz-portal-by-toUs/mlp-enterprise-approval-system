package com.multi.mlpenterpriseapprovalsystem.provdocument.service;

import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import com.multi.mlpenterpriseapprovalsystem.provdocument.event.ProvDocumentApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 알림 비동기 전송 서비스
 *
 * @author : 김승기
 * @filename : ProvNotificationService
 * @since : 2026. 1. 17. 토요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProvNotificationService {
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDocApprovedEvent(ProvDocumentApprovedEvent event) {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        log.info("[Async-Start] Thread: {}, event: {}", threadName, event.docTitle());

        List<Employee> emps = employeeRepository.findAllByCompany_ComId(event.comId());
        log.info("[Async-QueryDone] Thread: {}, Employee Count: {}, Time: {}ms",
                threadName, emps.size(), (System.currentTimeMillis() - startTime));

        for (Employee emp : emps) {
            notificationService.sendNotification(
                    emp, NotificationType.OTHER,
                    "챗봇 사용 가능 알림",
                    "이제부터 \"" + event.docTitle() + "\" 에 대한 질의를 챗봇에서 사용할 수 있습니다.",
                    "/employees"
            );
        }
        log.info("[Async-End] Thread: {}, Total Time: {}ms",
                threadName, (System.currentTimeMillis() - startTime));
    }
}