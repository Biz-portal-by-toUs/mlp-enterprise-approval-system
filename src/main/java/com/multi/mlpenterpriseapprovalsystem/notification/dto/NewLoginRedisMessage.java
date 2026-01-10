package com.multi.mlpenterpriseapprovalsystem.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 새 로그인에 대한 서버-서버 메시지
 *
 * @author : 권지영
 * @filename : NewLoginRedisMessage
 * @since : 2026. 1. 10. 토요일
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NewLoginRedisMessage {
    private String empId;
    private String deviceId;
    private String ip;
    private String at;
}