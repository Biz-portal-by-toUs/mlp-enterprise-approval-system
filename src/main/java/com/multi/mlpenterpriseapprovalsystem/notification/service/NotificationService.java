package com.multi.mlpenterpriseapprovalsystem.notification.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.sse.SseManager;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import com.multi.mlpenterpriseapprovalsystem.employee.enums.MsgStat;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.Notifications;
import com.multi.mlpenterpriseapprovalsystem.notification.dto.NotificationResponseDto;
import com.multi.mlpenterpriseapprovalsystem.notification.repository.NotificationsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 알림 서비스
 *
 * @author : 김승기
 * @filename : NotificationService
 * @since : 2026. 1. 3. 토요일
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationsRepository notificationsRepository;
    private final SseManager sseManager;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public void sendNotification(Employee receiver, NotificationType type, String title, String content, String url) {
        Notifications noti = Notifications.builder()
                .receiver(receiver)
                .company(receiver.getCompany())
                .notificationType(type)
                .content(content)
                .title(title)
                .url(url)
                .build();
        notificationsRepository.save(noti);

        long unreadCount = notificationsRepository.countByReceiver_EmpIdAndIsReadFalse(receiver.getEmpId());

        if (receiver.getMsgStat()== MsgStat.FOCUS){
            return;
        }


        Map<String, Object> data = new HashMap<>();
        data.put("notification", NotificationResponseDto.fromEntity(noti));
        data.put("unreadCount", unreadCount);

        sseManager.sendToUser(receiver.getEmpId(), "notification", data);
    }

    @Transactional
    public void sendNotification(String empId, NotificationType type, String title, String content, String url) {
        Employee receiver = employeeRepository.findByEmpId(empId)
                .orElseThrow(() -> new CustomException(ErrorCode.EMPLOYEE_NOT_FOUND));
        this.sendNotification(receiver, type, title, content, url);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotifications(String empId, NotificationType type, LocalDateTime cursorAt, Long cursorId, int size) {
        Pageable pageable = PageRequest.of(0, size);

        List<Notifications> list = notificationsRepository.findNotificationsByCursor(empId, type, cursorAt, cursorId, pageable);

        return list.stream()
                .map(NotificationResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notiNo, String empId) {
        Notifications notification = notificationsRepository.findById(notiNo)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getEmpId().equals(empId)) {
            throw new CustomException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notification.markAsRead();
    }

    /**
     * 전체 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(String empId) {
        List<Notifications> unreadNotifications = notificationsRepository
                .findByReceiver_EmpIdAndIsReadFalse(empId);

        unreadNotifications.forEach(Notifications::markAsRead);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String empId) {
        return notificationsRepository.countByReceiver_EmpIdAndIsReadFalse(empId);
    }

    public SseEmitter connectUserStream(String empId) {
        // connect는 SseManager에 구현되어 있어야 함
        return sseManager.connect(empId);
    }


    @Transactional
    public void deleteNotification(String username, Long notiNo) {
        Notifications notification = notificationsRepository.findById(notiNo)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getReceiver().getEmpId().equals(username)) {
            throw new CustomException(ErrorCode.NOTIFICATION_ACCESS_DENIED);
        }

        notificationsRepository.delete(notification);
    }
}