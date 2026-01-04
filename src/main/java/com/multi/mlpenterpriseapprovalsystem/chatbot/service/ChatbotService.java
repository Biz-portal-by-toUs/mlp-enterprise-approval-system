package com.multi.mlpenterpriseapprovalsystem.chatbot.service;

import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.ChatbotMessage;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.MessageRole;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.MessageStatus;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotCallbackDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ResChatbotMessageCreatedDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.repository.ChatbotMessageRepository;
import com.multi.mlpenterpriseapprovalsystem.common.sse.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 챗봇 서비스
 *
 * @author : 김승기
 * @filename : ChatbotService
 * @since : 2026. 1. 1. 목요일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotMessageRepository messageRepository;
    private final SseManager sseManager;

    /**
     * 질문 저장 및 답변용 placeholder 생성
     */
    public ResChatbotMessageCreatedDto createQuestionAndPrepareAnswer(ReqChatbotMessageDto req, String empId) {
        ChatbotMessage userMsg = ChatbotMessage.user(empId, req.getQuestion());
        messageRepository.save(userMsg);

        ChatbotMessage assistant = ChatbotMessage.assistantStreaming(empId);
        assistant = messageRepository.save(assistant);

        return new ResChatbotMessageCreatedDto(assistant.getId());
    }

    /**
     * 유저 SSE 연결 및 스트리밍 버퍼 복구
     */
    public SseEmitter connectUserStream(String empId) {
        // 공통 매니저를 통해 에미터 생성
        SseEmitter emitter = sseManager.createEmitter(empId);

        // 1. 초기 연결 메시지 발송
        sseManager.sendToUser(empId, "connected", Map.of(
                "empId", empId,
                "at", LocalDateTime.now().toString()
        ));

        // 2. 재연결 시 진행 중이던 스트리밍 데이터 밀어주기
        flushStreamingBuffer(empId, emitter);

        return emitter;
    }

    /**
     * FastAPI 콜백 처리 (청크 전송)
     */
    public void handleCallback(ReqChatbotCallbackDto cb) {
        if (cb.getMessageId() == null || cb.getMessageId().isBlank()) return;

        ChatbotMessage assistant = messageRepository.findById(cb.getMessageId()).orElse(null);
        if (assistant == null) return;

        String empId = assistant.getEmpId();

        // 처리 실패 시
        if (Boolean.FALSE.equals(cb.getSuccess())) {
            String msg = (cb.getErrorMessage() == null || cb.getErrorMessage().isBlank()) ? "AI 처리 실패" : cb.getErrorMessage();
            assistant.markError(msg);
            messageRepository.save(assistant);
            sseManager.sendToUser(empId, "error", Map.of("messageId", assistant.getId(), "message", msg));
            return;
        }

        // 청크(데이터 조각) 발송
        if (cb.getChunk() != null && !cb.getChunk().isBlank()) {
            assistant.appendChunk(cb.getChunk());
            messageRepository.save(assistant);
            sseManager.sendToUser(empId, "chunk", Map.of("messageId", assistant.getId(), "delta", cb.getChunk()));
        }

        // 스트리밍 종료
        if (Boolean.TRUE.equals(cb.getDone())) {
            assistant.markDone();
            messageRepository.save(assistant);
            sseManager.sendToUser(empId, "done", Map.of("messageId", assistant.getId()));
        }
    }

    /**
     * 새로 고침 시 진행 중이던 텍스트 복구
     */
    private void flushStreamingBuffer(String empId, SseEmitter emitter) {
        try {
            List<ChatbotMessage> streaming = messageRepository.findByEmpIdAndRoleAndStatusOrderByUpdatedAtDesc(
                    empId, MessageRole.ASSISTANT, MessageStatus.STREAMING, PageRequest.of(0, 3));

            for (ChatbotMessage m : streaming) {
                if (m.getContent() == null || m.getContent().isBlank()) continue;
                // 해당 탭(emitter)에만 직접 발송
                emitter.send(SseEmitter.event().name("buffer").data(Map.of(
                        "messageId", m.getId(),
                        "text", m.getContent()
                )));
            }
        } catch (Exception e) {
            log.warn("[ChatbotService] Buffer flush failed for empId={}", empId);
        }
    }

    /**
     * 과거 메시지 조회 (커서 기반 페이징)
     */
    public List<ChatbotMessage> getMessages(String empId, int limit, LocalDateTime beforeAt, String beforeId) {
        int safe = Math.max(1, Math.min(limit, 100));
        Pageable pageable = PageRequest.of(0, safe, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id")));

        if (beforeAt == null || beforeId == null || beforeId.isBlank()) {
            return messageRepository.findByEmpId(empId, pageable);
        }
        return messageRepository.findBeforeCursor(empId, beforeAt, beforeId, pageable);
    }
}