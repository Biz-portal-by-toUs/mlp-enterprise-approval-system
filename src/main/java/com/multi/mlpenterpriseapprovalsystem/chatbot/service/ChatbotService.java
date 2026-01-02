package com.multi.mlpenterpriseapprovalsystem.chatbot.service;

import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.ChatbotMessage;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.MessageRole;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.MessageStatus;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotCallbackDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ResChatbotMessageCreatedDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.repository.ChatbotMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /**
     * ✅ 유저(empId)당 SSE 1개가 기본
     * 멀티탭 고려해서 List로 관리
     */
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    /**
     * 질문 저장 + assistant placeholder 생성(STREAMING)
     * => assistantMessageId를 FastAPI에 넘겨서 콜백 키로 사용
     */
    public ResChatbotMessageCreatedDto createQuestionAndPrepareAnswer(ReqChatbotMessageDto req, String empId) {


        ChatbotMessage userMsg = ChatbotMessage.user(empId, req.getQuestion());
        messageRepository.save(userMsg);


        ChatbotMessage assistant = ChatbotMessage.assistantStreaming(empId);
        assistant = messageRepository.save(assistant);


        return new ResChatbotMessageCreatedDto(assistant.getId());
    }

    /**
     * ✅ 유저 SSE 연결
     * GET /api/v1/chatbot/stream
     */
    public SseEmitter connectUserStream(String empId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        userEmitters.computeIfAbsent(empId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(empId, emitter));
        emitter.onTimeout(() -> removeEmitter(empId, emitter));
        emitter.onError(ex -> removeEmitter(empId, emitter));

        safeSend(emitter, "connected", Map.of(
                "empId", empId,
                "at", LocalDateTime.now().toString()
        ));

        // 재연결 복구: 최근 STREAMING 1개 정도 buffer로 밀어줌(원하면 N개로)
        flushStreamingBuffer(empId, emitter);

        return emitter;
    }

    /**
     * FastAPI -> Spring 콜백
     * messageId는 assistantMessageId(Mongo _id)
     */
    public void handleCallback(ReqChatbotCallbackDto cb) {
        if (cb.getMessageId() == null || cb.getMessageId().isBlank()) return;

        ChatbotMessage assistant = messageRepository.findById(cb.getMessageId()).orElse(null);
        if (assistant == null) {
            log.warn("[ChatbotService] callback ignored: message not found id={}", cb.getMessageId());
            return;
        }

        String empId = assistant.getEmpId();

        // 실패
        if (Boolean.FALSE.equals(cb.getSuccess())) {
            String msg = (cb.getErrorMessage() == null || cb.getErrorMessage().isBlank())
                    ? "AI 처리 실패"
                    : cb.getErrorMessage();

            assistant.markError(msg);
            messageRepository.save(assistant);

            sendToUser(empId, "error", Map.of(
                    "messageId", assistant.getId(),
                    "message", msg
            ));
            return;
        }


        if (cb.getChunk() != null && !cb.getChunk().isBlank()) {
            assistant.appendChunk(cb.getChunk());
            messageRepository.save(assistant);

            sendToUser(empId, "chunk", Map.of(
                    "messageId", assistant.getId(),
                    "delta", cb.getChunk()
            ));
        }

        // done
        if (Boolean.TRUE.equals(cb.getDone())) {
            assistant.markDone();
            messageRepository.save(assistant);

            sendToUser(empId, "done", Map.of(
                    "messageId", assistant.getId()
            ));
        }
    }

    public List<ChatbotMessage> getMessages(String empId, int limit, LocalDateTime beforeAt, String beforeId) {
        int safe = Math.max(1, Math.min(limit, 100));
        Pageable pageable = PageRequest.of(
                0, safe,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("_id"))
        );

        if (beforeAt == null || beforeId == null || beforeId.isBlank()) {
            // 초기: 최신 N개
            return messageRepository.findByEmpId(empId, pageable);
        }

        // 커서: 이전 페이지
        return messageRepository.findBeforeCursor(empId, beforeAt, beforeId, pageable);
    }


    private void flushStreamingBuffer(String empId, SseEmitter emitter) {
        try {
            List<ChatbotMessage> streaming =
                    messageRepository.findByEmpIdAndRoleAndStatusOrderByUpdatedAtDesc(
                            empId, MessageRole.ASSISTANT, MessageStatus.STREAMING, PageRequest.of(0, 3)
                    );

            for (ChatbotMessage m : streaming) {
                if (m.getContent() == null || m.getContent().isBlank()) continue;
                safeSend(emitter, "buffer", Map.of(
                        "messageId", m.getId(),
                        "text", m.getContent()
                ));
            }
        } catch (Exception e) {
            log.warn("[ChatbotService] flushStreamingBuffer failed empId={}", empId, e);
        }
    }

    private void sendToUser(String empId, String eventName, Object data) {
        List<SseEmitter> emitters = userEmitters.get(empId);
        if (emitters == null || emitters.isEmpty()) return;

        for (SseEmitter emitter : emitters) {
            try {
                safeSend(emitter, eventName, data);
            } catch (RuntimeException ex) {
                removeEmitter(empId, emitter);
            }
        }
    }

    private void safeSend(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeEmitter(String empId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = userEmitters.get(empId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) userEmitters.remove(empId);
        }
        try { emitter.complete(); } catch (Exception ignore) {}
    }
}