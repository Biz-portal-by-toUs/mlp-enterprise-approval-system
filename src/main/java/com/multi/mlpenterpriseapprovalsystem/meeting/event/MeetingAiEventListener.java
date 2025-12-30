package com.multi.mlpenterpriseapprovalsystem.meeting.event;

import com.multi.mlpenterpriseapprovalsystem.common.client.MeetingAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 비동기 처리 eventListener
 *
 * @author : 김승기
 * @filename : MeetingAiEventListener
 * @since : 2025. 12. 30. 화요일
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeetingAiEventListener {

    private final MeetingAiClient meetingAiClient;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MeetingAiRequestedEvent e) {
        try {
            meetingAiClient.requestAi(e.meetNo(), e.objectKey(), e.title());
        } catch (Exception ex) {
            // 여기서 실패해도 DB는 이미 커밋됨
            log.error("[AI REQUEST FAILED] meetNo={}, objectKey={}, title={}", e.meetNo(), e.objectKey(), e.title(), ex);
        }
    }
}