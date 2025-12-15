package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.Builder;
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
@Builder
public class ErrorResponse {
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
