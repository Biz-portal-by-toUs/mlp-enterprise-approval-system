package com.multi.mlpenterpriseapprovalsystem.notification.domain;

import com.multi.mlpenterpriseapprovalsystem.common.domain.BaseEntity;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.employee.domain.Employee;
import jakarta.persistence.*;
import lombok.*;

/**
 * 알림함 엔티티
 *
 * @author : 김승기
 * @filename : notifications
 * @since : 2026. 1. 3. 토요일
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications")
public class Notifications extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "noti_no")
    private Long notiNo;

    @Column(name = "content", nullable = false, length = 255)
    private String content;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_type", nullable = false, length = 20)
    private NotificationType notificationType=NotificationType.OTHER;

    @Setter
    @Column(name = "url", nullable = false, length = 255)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", referencedColumnName = "com_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", referencedColumnName = "emp_id", nullable = false)
    private Employee receiver;

    @Builder // 빌더 패턴 추가
    public Notifications(String content, String title, String url, NotificationType notificationType, Company company, Employee receiver) {
        this.content = content;
        this.title = title;
        this.url = url;
        // 파라미터명을 필드명과 맞췄습니다.
        this.notificationType = notificationType != null ? notificationType : NotificationType.OTHER;
        this.company = company;
        this.receiver = receiver;
        this.isRead = false; // 신규 알림은 기본적으로 읽지 않음 상태
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
