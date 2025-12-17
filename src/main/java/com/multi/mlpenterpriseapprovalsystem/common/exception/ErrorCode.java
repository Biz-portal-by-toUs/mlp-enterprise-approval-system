package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Please explain the class!!!
 * HTTP 상태 코드, 에러 코드(프론트에서 어떤 예외인지 체킹 용도), 에러 메시지(사용자에게 화면에서 보여줄 용도)
 *
 * @author : 이지헌
 * @filename : ErrorCode
 * @since : 25. 12. 15. 월요일
 */
@Getter
public enum ErrorCode {

    // 인증 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다"),

    // 문서 관련
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다"),
    DOCUMENT_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "DOCUMENT_ALREADY_APPROVED", "이미 승인된 문서입니다"),
    INVALID_APPROVAL_STATUS(HttpStatus.BAD_REQUEST, "INVALID_APPROVAL_STATUS", "잘못된 결재 상태입니다"),

    // 권한 관련
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다"),
    NOT_DOCUMENT_OWNER(HttpStatus.FORBIDDEN, "NOT_DOCUMENT_OWNER", "문서 작성자만 수정할 수 있습니다"),

    // 서버 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다"),

    // @Valid 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 잘못되었습니다."),

    // 사원 관련 (기존 USER_NOT_FOUND와 구분하거나 통합하여 사용)
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "사원을 찾을 수 없습니다"),

    // 채팅 관련
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다"),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_ACCESS_DENIED", "해당 채팅방에 접근할 권한이 없습니다"),
    INVALID_MEMBER_COUNT(HttpStatus.BAD_REQUEST, "INVALID_MEMBER_COUNT", "채팅방 멤버는 최소 1명 이상이어야 합니다"),
    ALREADY_CHAT_MEMBER(HttpStatus.CONFLICT, "ALREADY_CHAT_MEMBER", "이미 채팅방에 참여 중인 멤버입니다");



    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
