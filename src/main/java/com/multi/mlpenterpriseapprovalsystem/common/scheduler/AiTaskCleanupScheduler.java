package com.multi.mlpenterpriseapprovalsystem.common.scheduler;

import com.multi.mlpenterpriseapprovalsystem.meeting.domain.AiStatus;
import com.multi.mlpenterpriseapprovalsystem.meeting.repository.MeetingRepository;
import com.multi.mlpenterpriseapprovalsystem.meeting.service.MeetingService;
import com.multi.mlpenterpriseapprovalsystem.provdocument.repository.ProvDocumentRepository;
import com.multi.mlpenterpriseapprovalsystem.provdocument.service.ProvDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 회의 및 사내 규정 상태 처리 스케줄러
 *
 * @author : 김승기
 * @filename : AiTaskCleanupScheduler
 * @since : 2026. 1. 12. 월요일
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiTaskCleanupScheduler {

    private final MeetingRepository meetingRepository;
    private final MeetingService meetingService;
    private final ProvDocumentRepository provDocumentRepository;
    private final ProvDocumentService provDocumentService;

    /**
     * 30분마다 실행: 'PROCESSING' 상태로 1시간 이상 방치된 작업들 강제 실패 처리
     */
    @Scheduled(cron = "0 0/30 * * * *")
    @Transactional
    public void cleanupStuckAiTasks() {
        LocalDateTime limitTime = LocalDateTime.now().minusHours(1);

        // 1. 회의 요약 정리
        meetingRepository.findAllByAiStatusAndUpdatedAtBefore(AiStatus.PROCESSING, limitTime)
                .forEach(m -> {
                    log.warn("[STUCK TASK] 회의 요약 강제 종료 - meetNo: {}", m.getMeetNo());
                    meetingService.handleAiRequestFailure(m.getMeetNo(), "AI 분석 시간 초과 (서버 응답 없음)");
                });

        // 2. 임베딩 작업 정리
        provDocumentRepository.findAllByProcStatAndUpdatedAtBefore("PROCESSING", limitTime)
                .forEach(doc -> {
                    log.warn("[STUCK TASK] 임베딩 강제 종료 - provNo: {}", doc.getProvNo());
                    provDocumentService.handleEmbeddingFailure(doc.getProvNo(), "임베딩 처리 시간 초과 (서버 응답 없음)");
                });
    }
}