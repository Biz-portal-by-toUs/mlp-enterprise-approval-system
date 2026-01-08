package com.multi.mlpenterpriseapprovalsystem.meeting.event;

import com.multi.mlpenterpriseapprovalsystem.common.client.MeetingAiClient;
import com.multi.mlpenterpriseapprovalsystem.meeting.service.MeetingService;
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
    private final MeetingService meetingService; // 서비스 주입

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MeetingAiRequestedEvent e) {
        try {
            meetingAiClient.requestAi(e.meetNo(), e.objectKey(), e.title());
            log.info("[AI REQUEST SUCCESS] meetNo={}", e.meetNo());
        } catch (Exception ex) {
            log.error("[AI REQUEST FAILED] meetNo={}, reason={}", e.meetNo(), ex.getMessage(), ex);

            try {
                meetingService.handleAiRequestFailure(e.meetNo(), "AI 서버 연결 실패: " + ex.getMessage());
            } catch (Exception dbEx) {
                log.error("[CRITICAL] Failed to update AI status to FAILED for meetNo={}", e.meetNo(), dbEx);
            }
        }
    }
}