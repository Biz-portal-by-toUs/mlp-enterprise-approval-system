package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Please explain the class!!!
 * 예외 발생 시 프론트로 전달할 객체.
 * 프론트에선 이 객체만 있으면 필요한 모든 데이터를 얻을 수 있다.
 *
 * @author : 이지헌
 * @filename : ErrorResponse
 * @since : 25. 12. 15. 월요일
 */
// 클라이언트에게 줄 응답 형태 통일

@Getter
public class ErrorResponse {
    private final int status;
    private final String code;
    private final String message;

    public ErrorResponse(HttpStatus status, String code, String message) {
        this.status = status.value();
        this.code = code;
        this.message = message;
    }
}
