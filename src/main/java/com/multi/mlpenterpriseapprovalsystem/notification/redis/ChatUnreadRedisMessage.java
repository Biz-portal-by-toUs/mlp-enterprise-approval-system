package com.multi.mlpenterpriseapprovalsystem.notification.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 안읽은 메세지 Dto
 *
 * @author : 김승기
 * @filename : ChatUnreadRedisMessage
 * @since : 2026. 1. 18. 일요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatUnreadRedisMessage {
    private String empId;
    private long totalCount;
}