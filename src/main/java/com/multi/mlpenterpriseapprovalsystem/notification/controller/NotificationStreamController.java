package com.multi.mlpenterpriseapprovalsystem.notification.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 sse controller
 *
 * @author : 김승기
 * @filename : NotificationStreamController
 * @since : 2026. 1. 5. 월요일
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationStreamController {

    private final NotificationService notificationService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal CustomUser user) {
        return notificationService.connectUserStream(user.getUsername());
    }

    @GetMapping(value = "/login-detect-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter loginDetectStream(@AuthenticationPrincipal CustomUser user,
                                        @RequestParam(name="deviceId") String deviceId) {
        return notificationService.connectLoginDetectStream(user.getUsername(), deviceId);
    }
}