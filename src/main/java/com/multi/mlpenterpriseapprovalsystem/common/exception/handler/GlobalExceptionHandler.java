package com.multi.mlpenterpriseapprovalsystem.common.exception.handler;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : GlobalExceptionHandler
 * @since : 25. 12. 15. 월요일
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 커스텀 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode code = e.getErrorCode();

        log.warn("[CustomException] {}: {}: {}", code.name(), code.getCode(), code.getMessage());

        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getCode(), code.getMessage()));
    }

    // 일반 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("[Unhandled Exception] {}", e);

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "알 수 없는 서버 오류가 발생했습니다."));
    }
}
