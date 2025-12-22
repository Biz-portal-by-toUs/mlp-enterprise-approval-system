package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.Getter;

/**
 * 런타임예외를 상속한 공통 예외
 *
 * 사용예시:
 * Document document = documentRepository.findById(id)
 *      .orElseThrow(() -> new CustomException(ErrorCode.DOCUMENT_NOT_FOUND));
 *
 * @author : 이지헌
 * @filename : CustomException
 * @since : 25. 12. 15. 월요일
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
