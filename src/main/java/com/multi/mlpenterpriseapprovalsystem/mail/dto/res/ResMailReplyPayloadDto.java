package com.multi.mlpenterpriseapprovalsystem.mail.dto.res;

import java.util.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ResMailReplyPayloadDto
 * @since : 2026-01-09 금요일
 */
public record ResMailReplyPayloadDto(
        String payloadKey,   // 중복 적용 방지용 키 (서버가 생성)
        Long replyToMailNo,  // 원문 mailNo
        String title,        // RE: 처리된 제목
        List<ReceiverItem> receivers,
        String quoteCnttJson, // 원문 tiptap JSON (mail.getCntt())
        String originalTitle,
        String quoteHtml,
        String originalSenderEmpId,
        String originalSenderEmpName
) {
    public record ReceiverItem(String empId, String empName) {}
}