package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multi.mlpenterpriseapprovalsystem.common.sse.SseManager;
import com.multi.mlpenterpriseapprovalsystem.employee.enums.MsgStat;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.Notifications;
import com.multi.mlpenterpriseapprovalsystem.notification.dto.ResNotificationDto;
import com.multi.mlpenterpriseapprovalsystem.notification.repository.NotificationsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 알림 subscriber
 *
 * @author : 김승기
 * @filename : RedisNotificationSubscriber
 * @since : 2026. 1. 6. 화요일
 */
@Component
@Slf4j
public class RedisNotificationSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final NotificationsRepository notificationsRepository;
    private final EmployeeRepository employeeRepository;
    private final SseManager sseManager;

    // 수동 생성자 추가
    public RedisNotificationSubscriber(
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper, // 사용할 빈 이름 지정
            NotificationsRepository notificationsRepository,
            EmployeeRepository employeeRepository,
            SseManager sseManager
    ) {
        this.objectMapper = objectMapper;
        this.notificationsRepository = notificationsRepository;
        this.employeeRepository = employeeRepository;
        this.sseManager = sseManager;
    }

    /**
     * Redis Pub/Sub 메시지 수신 엔트리 포인트
     * (MessageListenerAdapter가 이 메서드를 호출)
     */
    @Override
    @Transactional(readOnly = true)
    public void onMessage(Message message, byte[] pattern) {
        System.out.println("[NOTI SUB] hit");
        String raw = new String(message.getBody(), StandardCharsets.UTF_8);
        log.info("raw={}", raw);

        final NotificationRedisMessage msg;
        try {
            msg = objectMapper.readValue(raw, NotificationRedisMessage.class);
        } catch (Exception e) {
            return;
        }

        String empId = msg.getEmpId();
        Long notiNo = msg.getNotiNo();

        System.out.println("=============================EMPID NOTINO "+ empId + notiNo);

        boolean focus = employeeRepository.findByEmpId(empId)
                .map(emp -> emp.getMsgStat() == MsgStat.FOCUS)
                .orElse(false);
        if (focus) return;

        Notifications noti = notificationsRepository.findById(notiNo).orElse(null);
        if (noti == null) return;

        long unreadCount = notificationsRepository.countByReceiver_EmpIdAndIsReadFalse(empId);

        Map<String, Object> data = new HashMap<>();
        data.put("notification", ResNotificationDto.fromEntity(noti));
        data.put("unreadCount", unreadCount);

        sseManager.sendToUser(empId, "notification", data);
    }
}