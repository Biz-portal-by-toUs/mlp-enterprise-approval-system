package com.multi.mlpenterpriseapprovalsystem.meeting.event;

/**
 * 이벤트 처리 클래스
 *
 * @author : 김승기
 * @filename : MeetingAiRequestedEvent
 * @since : 2025. 12. 30. 화요일
 */

public record MeetingAiRequestedEvent(Long meetNo, String objectKey, String title) {
}