package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.Getter;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : CustomException
 * @since : 25. 12. 15. 월요일
 */
// 런타임예외를 상속한 공통 예외
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
