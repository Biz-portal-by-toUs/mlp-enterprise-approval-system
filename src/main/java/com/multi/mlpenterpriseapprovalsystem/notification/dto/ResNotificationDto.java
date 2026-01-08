package com.multi.mlpenterpriseapprovalsystem.notification.dto;

import com.multi.mlpenterpriseapprovalsystem.notification.domain.NotificationType;
import com.multi.mlpenterpriseapprovalsystem.notification.domain.Notifications;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림함 resDto
 *
 * @author : 김승기
 * @filename : NotificationResponseDto
 * @since : 2026. 1. 3. 토요일
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResNotificationDto {

    private Long notiNo;           // 알림 고유 번호 (Cursor ID로 사용)
    private String content;        // 알림 내용
    private String url;            // 클릭 시 이동할 링크
    private String title;
    private boolean isRead;        // 읽음 여부
    private NotificationType type; // 알림 타입 (APPROVAL, CHAT, etc.)
    private LocalDateTime createdAt; // 생성 일시 (Cursor DateTime으로 사용)

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static ResNotificationDto fromEntity(Notifications entity) {
        return ResNotificationDto.builder()
                .notiNo(entity.getNotiNo())
                .content(entity.getContent())
                .title(entity.getTitle())
                .url(entity.getUrl())
                .isRead(entity.getIsRead())
                .type(entity.getNotificationType())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}