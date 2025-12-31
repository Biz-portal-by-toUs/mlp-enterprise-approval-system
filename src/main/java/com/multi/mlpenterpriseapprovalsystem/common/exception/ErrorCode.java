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
    DUPLICATE_COMID(HttpStatus.CONFLICT, "DUPLICATE_COMID", "이미 사용 중인 회사 코드입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다"),
    INVALID_BRN(HttpStatus.BAD_REQUEST, "INVALID_BRN", "유효하지 않은 사업자등록번호입니다"),
    BRN_DUPLICATE(HttpStatus.CONFLICT, "BRN_DUPLICATE", "이미 존재하는 사업자등록번호입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다"),
    ALREADY_USE_PASSWORD(HttpStatus.BAD_REQUEST, "ALREADY_USE_PASSWORD", "이미 사용중인 비밀번호입니다"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "입력정보가 일치하지 않습니다"),
    NOT_BLANK(HttpStatus.BAD_REQUEST, "NOT_BLANK", "입력이 필수인 칸 입니다"),

    //요금제 관련
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "요금제를 찾을 수 없습니다"),

    // 문서 관련
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "문서를 찾을 수 없습니다"),
    DOCUMENT_ALREADY_APPROVED(HttpStatus.BAD_REQUEST, "DOCUMENT_ALREADY_APPROVED", "이미 승인된 문서입니다"),
    INVALID_APPROVAL_STATUS(HttpStatus.BAD_REQUEST, "INVALID_APPROVAL_STATUS", "잘못된 결재 상태입니다"),
    INVALID_DOCUMENT_STATUS_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_STATUS", "잘못된 문서 상태 파라미터 값이 전달되었습니다"),
    INVALID_DOCUMENT_SORT_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_DOCUMENT_SORT_REQUEST", "잘못된 문서 정렬 파라미터 값이 전달되었습니다"),
    DOCUMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "DOCUMENT_ACCESS_DENIED", "해당 문서에 접근 권한이 없습니다"),
    DOCUMENT_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "DOCUMENT_ALREADY_PROCESSED", "이미 결재가 진행된 문서는 상신 취소할 수 없습니다"),
    DOCUMENT_NOT_AWAITING(HttpStatus.BAD_REQUEST, "DOCUMENT_NOT_AWAITING", "결재 대기중인 문서가 아닙니다"),
    DOCUMENT_SERIAL_OVERFLOW(HttpStatus.INTERNAL_SERVER_ERROR, "DOCUMENT_SERIAL_OVERFLOW", "문서 일련번호가 최대치를 초과했습니다"),
    DOCUMENT_NOT_TEMP(HttpStatus.BAD_REQUEST, "DOCUMENT_NOT_TEMP", "임시저장 상태의 문서만 수정할 수 있습니다."),
    DOCUMENT_NOT_REJECTED(HttpStatus.BAD_REQUEST, "DOCUMENT_NOT_REJECTED", "반려된 문서만 재작성할 수 있습니다."),
    CONTENT_TOO_SHORT_FOR_SUMMARY(HttpStatus.BAD_REQUEST, "CONTENT_TOO_SHORT_FOR_SUMMARY", "요약하기엔 내용이 너무 짧습니다 (최소 100자)"),
    AI_SUMMARY_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI_SUMMARY_GENERATION_FAILED", "AI 요약 생성에 실패했습니다"),

    // 결재라인 관련
    INVALID_APPROVAL_LINE_ORDER(HttpStatus.BAD_REQUEST, "INVALID_APPROVAL_LINE_ORDER", "잘못된 결재자 순서입니다"),
    INVALID_APPROVAL_LINE_SELF(HttpStatus.BAD_REQUEST, "INVALID_APPROVAL_LINE_SELF", "본인은 결재라인에 포함될 수 없습니다"),
    APPROVAL_LINE_NOT_FOUND(HttpStatus.NOT_FOUND, "APPROVAL_LINE_NOT_FOUND", "해당 문서의 결재라인에 포함되어 있지 않습니다"),
    NOT_MY_TURN_TO_APPROVE(HttpStatus.BAD_REQUEST, "NOT_MY_TURN_TO_APPROVE", "아직 결재 차례가 아닙니다"),
    REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "REJECT_REASON_REQUIRED", "반려 사유를 입력해주세요"),

    // 문서 양식 관련
    DOCUMENT_FORM_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_FORM_NOT_FOUND", "문서 양식을 찾을 수 없습니다"),

    // 문서 양식 내 카테고리 관련
    DOCUMENT_FORM_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "DOCUMENT_FORM_CATEGORY_NOT_FOUND", "문서 양식 카테고리를 찾을 수 없습니다"),

    // 회사 관련
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "COMPANY_NOT_FOUNT", "회사를 찾을 수 없습니다"),
    ONLY_COMPANY(HttpStatus.BAD_REQUEST,"ONLY_COMPANY","회사 계정만 접근 가능합니다"),

    // 예약 관련
    MEETING_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING_ROOM_NOT_FOUND", "회의실을 찾을 수 없습니다"),
    DUPLICATE_MEETING_ROOM_NAME(HttpStatus.CONFLICT, "DUPLICATE_MEETING_ROOM_NAME", "이미 존재하는 회의실명입니다."),

    CORPORATE_CAR_NOT_FOUND(HttpStatus.NOT_FOUND, "CORPORATE_CAR_NOT_FOUND", "법인 차량을 찾을 수 없습니다"),
    DUPLICATE_CAR_PLATE_NO(HttpStatus.CONFLICT, "DUPLICATE_CAR_PLATE_NO", "이미 존재하는 차량 번호입니다."),

    SHARED_EQUIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "SHARED_EQUIPMENT_NOT_FOUND", "공유 설비를 찾을 수 없습니다"),

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
    INVALID_CURSOR(HttpStatus.BAD_REQUEST,"INVALID_CURSOR","커서값이 잘못되었습니다"),

    // 부서 관련
    DUPLICATE_DEPID(HttpStatus.CONFLICT, "DUPLICATE_DEPID", "이미 존재하는 부서 코드입니다"),
    DUPLICATE_DEPNAME(HttpStatus.CONFLICT, "DUPLICATE_DEPNAME", "이미 존재하는 부서 이름입니다"),
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND", "부서를 찾을 수 없습니다"),
    DEPARTMENT_DELETE_HAS_EMPLOYEES(HttpStatus.BAD_REQUEST, "DEPARTMENT_DELETE_HAS_EMPLOYEES", "사원이 존재하므로 삭제할 수 없습니다"),

    // 직급 관련
    DUPLICATE_POSNAME(HttpStatus.CONFLICT, "DUPLICATE_POSNAME", "이미 존재하는 직급 이름입니다"),
    POSITIONS_NOT_FOUND(HttpStatus.NOT_FOUND, "POSITIONS_NOT_FOUND", "직급을 찾을 수 없습니다"),
    POSITIONS_DELETE_HAS_EMPLOYEES(HttpStatus.BAD_REQUEST, "POSITIONS_DELETE_HAS_EMPLOYEES", "사원이 존재하므로 삭제할 수 없습니다"),



    // 사원 관련 (기존 USER_NOT_FOUND와 구분하거나 통합하여 사용)
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "사원을 찾을 수 없습니다"),
    EMPLOYEE_ALREADY_RETIRED(HttpStatus.BAD_REQUEST,"EMPLOYEE_ALREADY_RETIRED","이미 퇴사 처리된 사원입니다"),
    ONLY_EMPLOYEE(HttpStatus.BAD_REQUEST,"ONLY_EMPLOYEE","사원 계정만 접근 가능합니다"),


    // 웹소켓 관련
    SOCKET_AUTHENTICATION_ERROR(HttpStatus.UNAUTHORIZED, "SOCKET_AUTHENTICATION_ERROR", "웹소켓 인증 정보가 유효하지 않습니다"),

    // 서버/기술적 에러
    JSON_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "JSON_PARSING_ERROR", "메시지 데이터 변환 중 오류가 발생했습니다"),
    REDIS_PUB_SUB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_PUB_SUB_ERROR", "메시지 전송 시스템에 오류가 발생했습니다"),

    // 회의 관련
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND,"MEETING_NOT_FOUND","없거나 삭제된 회의입니다."),
    MEETING_ACCESS_DENIED(HttpStatus.BAD_REQUEST,"MEETING_ACCESS_DENIED","회의에 접근할 권한이 없습니다"),
    MEETING_EDIT_DELETE_DENIED(HttpStatus.BAD_REQUEST,"MEETING_EDIT_DELETE_DENIED","회의를 수정하거나 삭제할 권한이 없습니다"),
    MEETING_DEPT_REQUIRED(HttpStatus.NO_CONTENT,"MEETING_DEPT_REQUIRED","부서는 한개 이상 선택해야 합니다"),
    MEETING_PARTICIPANT_REQUIRED(HttpStatus.NO_CONTENT,"MEETING_PARTICIPANT_REQUIRED","참석자는 한명 이상 선택해야합니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","유효하지 않는 요청입니다"),

    // 챗봇 관련
    PROV_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND,"PROV_DOCUMENT_NOT_FOUND","없거나 삭제된 사내 규정 문서입니다"),

    // 채팅 관련
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_ROOM_NOT_FOUND", "채팅방을 찾을 수 없습니다"),
    COMPANY_MISMATCH(HttpStatus.BAD_REQUEST,"COMPANY_MISMATCH","맞지 않는 회사타입입니다"),
    CHAT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHAT_ACCESS_DENIED", "해당 채팅방에 접근할 권한이 없습니다"),
    INVALID_MEMBER_COUNT(HttpStatus.BAD_REQUEST, "INVALID_MEMBER_COUNT", "채팅방 멤버는 최소 1명 이상이어야 합니다"),
    ALREADY_CHAT_MEMBER(HttpStatus.CONFLICT, "ALREADY_CHAT_MEMBER", "이미 채팅방에 참여 중인 멤버입니다"),
    INVALID_ROOM_TYPE(HttpStatus.BAD_REQUEST,"INVALID_ROOM_TYPE","맞지 않는 채팅방타입입니다");




    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
