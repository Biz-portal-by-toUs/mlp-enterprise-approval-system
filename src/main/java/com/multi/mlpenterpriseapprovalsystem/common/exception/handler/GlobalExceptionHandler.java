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
 * 서비스계층에서 예외를 아래처럼 던지면,
 * CustomException안의 ErrorCode를 꺼내서 ErrorResponse에 넣고 프론트로 전달
 *
 * 사용예시:
 * Document document = documentRepository.findById(id)
 *       .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
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

        log.warn("[CustomException] {}: {}: {}: {}", code.name(), code.getStatus(), code.getCode(), code.getMessage());

        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getStatus(), code.getCode(), code.getMessage()));
    }

    // 일반 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("[Unhandled Exception]", e);

        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getStatus(), code.getCode(), code.getMessage()));
    }
}
