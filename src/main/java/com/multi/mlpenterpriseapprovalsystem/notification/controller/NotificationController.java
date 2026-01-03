package com.multi.mlpenterpriseapprovalsystem.notification.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.dto.NotificationResponseDto;
import com.multi.mlpenterpriseapprovalsystem.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 컨트롤러
 *
 * @author : 김승기
 * @filename : NotificationController
 * @since : 2026. 1. 3. 토요일
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<NotificationResponseDto>>> getNotifications(
            @RequestParam(name = "type", required = false) NotificationType type,
            @RequestParam(name = "lastNotifiedAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cursorAt,
            @RequestParam(name = "lastNotiNo", required = false) Long cursorId,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUser user) {

        String empId = user.getUsername();

        List<NotificationResponseDto> notifications =
                notificationService.getNotifications(empId, type, cursorAt, cursorId, size);

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "알림 조회 성공", notifications)
        );
    }

    /**
     * 알림 하나 읽음 처리
     */
    @PatchMapping("/{notiNo}/read")
    public ResponseEntity<ResponseDto<Void>> readNotification(
            @PathVariable(name = "notiNo") Long notiNo,
            @AuthenticationPrincipal CustomUser user) {

        notificationService.markAsRead(notiNo, user.getUsername());

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "알림 읽음 처리 성공", null)
        );
    }

    /**
     * 전체 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ResponseDto<Void>> readAllNotifications(
            @AuthenticationPrincipal CustomUser user) {

        notificationService.markAllAsRead(user.getUsername());

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "전체 알림 읽음 처리 성공", null)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ResponseDto<Long>> getUnreadCount(@AuthenticationPrincipal CustomUser user) {
        long count = notificationService.getUnreadCount(user.getUsername());
        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "안 읽은 알림 개수 조회 성공", count)
        );
    }

}
