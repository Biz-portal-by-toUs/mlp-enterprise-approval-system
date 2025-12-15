package com.multi.mlpenterpriseapprovalsystem.common.exception.handler;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // @Valid 예외 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        ErrorCode code = ErrorCode.INVALID_INPUT_VALUE;

        Map<String, String> errors = new ConcurrentHashMap<>();

        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            // 필드명(title), 메시지("제목은 필수입니다")
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getStatus(), code.getCode(), code.getMessage(), errors));
    }

    // 일반 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;

        log.error("[Unhandled Exception]", e);

        return ResponseEntity.status(code.getStatus())
                .body(new ErrorResponse(code.getStatus(), code.getCode(), code.getMessage()));
    }
}
