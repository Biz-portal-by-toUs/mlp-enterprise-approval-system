package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : ErrorResponse
 * @since : 25. 12. 15. 월요일
 */
// 클라이언트에게 줄 응답 형태 통일

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private final String code;
    private final String message;
}
