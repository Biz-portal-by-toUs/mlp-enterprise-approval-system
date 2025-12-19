package com.multi.mlpenterpriseapprovalsystem.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * HTTP 상태 코드, 에러 코드(프론트에서 어떤 예외인지 체킹 용도), 에러 메시지(사용자에게 화면에서 보여줄 용도)
 *
 * 에러 추가하기 전에 이미 있는 에러인지 확인부탁!!
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

    // 회사 관련
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUNT", "회사를 찾을 수 없습니다"),

    // 예약 관련
    MEETING_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING_ROOM_NOT_FOUND", "회의실을 찾을 수 없습니다"),

    // 파일 업로드 실패
    FILE_UPLOAD_FAILED(HttpStatus.BAD_REQUEST, "FILE_UPLOAD_FAILED", "파일 업로드에 실패했습니다"),

    // 권한 관련
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다"),
    NOT_DOCUMENT_OWNER(HttpStatus.FORBIDDEN, "NOT_DOCUMENT_OWNER", "문서 작성자만 수정할 수 있습니다"),

    // 포트원/결제 관련
    PORTONE_TOKEN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PORTONE_TOKEN_ERROR", "포트원 인증 토큰 발급에 실패했습니다"),
    PORTONE_PAYMENT_LOOKUP_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PORTONE_PAYMENT_LOOKUP_ERROR", "결제 정보 조회에 실패했습니다"),
    PORTONE_PAYMENT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "PORTONE_PAYMENT_ERROR", "결제 요청에 실패했습니다"),

    // 결제수단 관련
    PAYMENT_METHOD_NOT_FOUND(HttpStatus.NOT_FOUND, "PAYMENT_METHOD_NOT_FOUND", "등록된 결제수단이 없습니다"),
    PAYMENT_METHOD_DUPLICATE(HttpStatus.CONFLICT, "PAYMENT_METHOD_DUPLICATE", "이미 등록된 카드입니다"),
    BILLINGKEY_NOT_MATCH(HttpStatus.BAD_REQUEST, "BILLINGKEY_NOT_MATCH", "빌링키 정보가 일치하지 않습니다"),
    DUPLICATE_CARD(HttpStatus.CONFLICT, "DUPLICATE_CARD", "이미 등록된 카드입니다"),

    // 결제 실패
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "PAYMENT_FAILED", "결제에 실패했습니다"),
    PAYMENT_CANCELLED(HttpStatus.BAD_REQUEST, "PAYMENT_CANCELLED", "취소된 결제입니다"),

    // 서버 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다"),

    // @Valid 에러
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 잘못되었습니다."),

    // 사원 관련 (기존 USER_NOT_FOUND와 구분하거나 통합하여 사용)
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "사원을 찾을 수 없습니다"),

    // 웹소켓 관련
    SOCKET_AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED, "SOCKET_AUTHENTICATION_ERROR", "웹소켓 인증 정보가 유효하지 않습니다"),

    // 서버/기술적 에러
    JSON_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON_PARSING_ERROR", "메시지 데이터 변환 중 오류가 발생했습니다"),
    REDIS_PUB_SUB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_PUB_SUB_ERROR", "메시지 전송 시스템에 오류가 발생했습니다"),

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
