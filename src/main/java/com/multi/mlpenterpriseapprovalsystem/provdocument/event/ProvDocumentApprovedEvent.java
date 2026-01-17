package com.multi.mlpenterpriseapprovalsystem.provdocument.event;

/**
 * 알림 비동기 전송 이벤트
 *
 * @author : 김승기
 * @filename : ProvDocumentApprovedEvent
 * @since : 2026. 1. 17. 토요일
 */
public record ProvDocumentApprovedEvent(
        Long provNo,
        String docTitle,
        String comId
) {

}