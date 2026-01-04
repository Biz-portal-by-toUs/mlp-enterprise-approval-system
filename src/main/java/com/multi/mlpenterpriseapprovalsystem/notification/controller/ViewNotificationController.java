package com.multi.mlpenterpriseapprovalsystem.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 알림함 viewController
 *
 * @author : 김승기
 * @filename : ViewNotificationController
 * @since : 2026. 1. 3. 토요일
 */
@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class ViewNotificationController {
    /**
     * 알림 센터 메인 페이지 호출
     * URL: GET /notifications -> templates/notifications.html 반환
     */
    @GetMapping
    public String notificationPage() {
        return "notification/notifications"; // templates 폴더 아래의 notifications.html 파일을 찾습니다.
    }
}
