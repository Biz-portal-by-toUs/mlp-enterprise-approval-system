package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * redismessage dto
 *
 * @author : 김승기
 * @filename : NotificationRedisMessage
 * @since : 2026. 1. 6. 화요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRedisMessage {
    private String empId;
    private Long notiNo;
}