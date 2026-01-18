package com.multi.mlpenterpriseapprovalsystem.chatbot.service;

import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.AgentActionId;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.ChatbotMessage;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.ChatbotRequestFailedEvent;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotCallbackDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ResChatbotMessageCreatedDto;
import com.multi.mlpenterpriseapprovalsystem.common.client.ChatbotAiClient;
import com.multi.mlpenterpriseapprovalsystem.common.sse.SseManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 챗봇 서비스
 *
 * @author : 김승기
 * @filename : ChatbotService
 * @since : 2026. 1. 1. 목요일
 */
@Slf4j
@Service
public class ChatbotService {

    private final RedisTemplate<String, Object> redisTemplate; // Redis 문맥 관리용
    private final ChatbotAiClient chatbotAiClient;
    private final SseManager sseManager;

    public ChatbotService(@Qualifier("chatbotRedisTemplate") RedisTemplate<String, Object> redisTemplate, ChatbotAiClient chatbotAiClient, SseManager sseManager) {
        this.redisTemplate = redisTemplate;
        this.chatbotAiClient = chatbotAiClient;
        this.sseManager = sseManager;
    }

    private static final String CHAT_KEY_PREFIX = "chat:session:";
    private static final int CONTEXT_LIMIT = 10;

    /**
     * 질문 처리 메인 로직: Redis 저장 -> 문맥 추출(정제) -> AI 호출
     */
    public ResChatbotMessageCreatedDto processQuestion(ReqChatbotMessageDto req, String empId, String comId, String sessionId) {
        String key = CHAT_KEY_PREFIX + sessionId;

        ChatbotMessage userMsg = ChatbotMessage.user(empId, req.getQuestion());
        redisTemplate.opsForList().rightPush(key, userMsg);

        String assistantMsgId = String.format("%s_%s_%d", empId, sessionId, System.currentTimeMillis());

        ChatbotMessage assistant = ChatbotMessage.assistantStreaming(empId);
        assistant.setId(assistantMsgId);
        redisTemplate.opsForList().rightPush(key, assistant);

        List<Map<String, String>> history = getRefinedContext(sessionId);

        chatbotAiClient.requestChatbotAnswer(
                assistantMsgId,
                empId,
                comId,
                req.getQuestion(),
                history
        );

        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        return new ResChatbotMessageCreatedDto(assistantMsgId);
    }

    /**
     * AI에게 전달할 경량화된 대화 내역 조회
     * @return List<Map<String, String>> (role과 content만 포함)
     */
    public List<Map<String, String>> getRefinedContext(String sessionId) {
        String key = CHAT_KEY_PREFIX + sessionId;
        List<Object> history = redisTemplate.opsForList().range(key, 0, -1);

        if (history == null || history.isEmpty()) return List.of();

        return history.stream()
                .map(obj -> (ChatbotMessage) obj)
                // AI가 이해할 수 있는 최소한의 정보(role, content)만 추출
                .map(m -> Map.of(
                        "role", m.getRole().name().toLowerCase(),
                        "content", m.getContent()
                ))
                .limit(CONTEXT_LIMIT)
                .collect(Collectors.toList());
    }

    /**
     * FastAPI 콜백 처리 (청크 스트리밍 전송)
     */
    public void handleCallback(ReqChatbotCallbackDto cb) {
        String msgId = cb.getMessageId();
        if (msgId == null || msgId.isBlank()) return;

        String empId = extractEmpIdFromMessageId(msgId);
        String sessionId = extractSessionIdFromMessageId(msgId);

        String connectionKey = empId + ":" + sessionId;
        String bufferKey = "chat:buffer:" + msgId;

        if (Boolean.FALSE.equals(cb.getSuccess())) {
            String errorMsg = (cb.getErrorMessage() == null) ? "AI 답변 생성 중 오류가 발생했습니다." : cb.getErrorMessage();
            sseManager.sendToUser(connectionKey, "error", Map.of("messageId", msgId, "message", errorMsg));
            redisTemplate.delete(bufferKey);
            return;
        }

        if (cb.getChunk() != null) {
            redisTemplate.opsForZSet().add(bufferKey, cb.getChunk(), cb.getSeq());
            sseManager.sendToUser(connectionKey, "chunk", Map.of(
                    "messageId", msgId,
                    "delta", cb.getChunk(),
                    "seq", cb.getSeq()
            ));
        }

        if (Boolean.TRUE.equals(cb.getDone())) {
            String finalContent = cb.getFullText();
            System.out.println("$$$$$$$$$$$$$$$$"+finalContent);
            if (finalContent == null || finalContent.isBlank()) {
                Set<Object> parts = redisTemplate.opsForZSet().range(bufferKey, 0, -1);
                finalContent = parts == null ? "" : parts.stream().map(Object::toString).collect(Collectors.joining());
            }
            updateMessageInRedis(sessionId, msgId, finalContent);
            sseManager.sendToUser(connectionKey, "done", Map.of(
                    "messageId", msgId,
                    "fullText", finalContent
            ));
            redisTemplate.delete(bufferKey);
        }



        if (cb.getActionId() != null) {
            AgentActionId action = AgentActionId.valueOf(cb.getActionId());
            String url = action.getUrlTemplate();

            if (cb.getParams() != null) {
                for (Map.Entry<String, Object> entry : cb.getParams().entrySet()) {
                    url = url.replace("{" + entry.getKey() + "}", entry.getValue().toString());
                }
            }

            sseManager.sendToUser(connectionKey, "action", Map.of(
                    "messageId", cb.getMessageId(),
                    "actionId", action.name(),
                    "url", url,
                    "label", action.getDefaultLabel()
            ));
        }
    }

    /**
     * 유저 SSE 연결 관리 (탭별로 독립적인 통로 생성)
     */
    public SseEmitter connectUserStream(String empId, String sessionId) {
        String connectionKey = empId + ":" + sessionId;

        SseEmitter emitter = sseManager.createEmitter(connectionKey);

        sseManager.sendToUser(connectionKey, "connected", Map.of(
                "empId", empId,
                "sessionId", sessionId,
                "at", LocalDateTime.now().toString()
        ));

        return emitter;
    }

    // --- Helper Methods ---

    /**
     * Assistant ID (empId_sessionId_timestamp)에서 empId 추출
     */
    private String extractEmpIdFromMessageId(String messageId) {
        try {
            return messageId.split("_")[0];
        } catch (Exception e) {
            log.error("[ChatbotService] empId 파싱 실패: {}", messageId);
            return "unknown";
        }
    }

    /**
     * Assistant ID (empId_sessionId_timestamp)에서 sessionId 추출
     */
    private String extractSessionIdFromMessageId(String messageId) {
        try {
            return messageId.split("_")[1];
        } catch (Exception e) {
            log.error("[ChatbotService] sessionId 파싱 실패: {}", messageId);
            return "unknown";
        }
    }

    private void updateMessageInRedis(String sessionId, String msgId, String content) {
        String key = CHAT_KEY_PREFIX + sessionId;
        List<Object> history = redisTemplate.opsForList().range(key, 0, -1);

        if (history == null) return;

        for (int i = 0; i < history.size(); i++) {
            ChatbotMessage m = (ChatbotMessage) history.get(i);
            if (msgId.equals(m.getId())) {
                m.setContent(content);
                redisTemplate.opsForList().set(key, i, m);
                break;
            }
        }
    }

    @EventListener
    public void handleInstantFailure(ChatbotRequestFailedEvent event) {
        String msgId = event.messageId(); // 이벤트에서 msgId 추출

        String empId = extractEmpIdFromMessageId(msgId);
        String sessionId = extractSessionIdFromMessageId(msgId);
        String connectionKey = empId + ":" + sessionId;

        // 1. SSE로 유저에게 즉시 에러 전송
        sseManager.sendToUser(connectionKey, "error", Map.of(
                "messageId", msgId,
                "message", "AI 서버와 연결할 수 없습니다. 잠시 후 다시 시도해주세요."
        ));

        // 2. Redis에 남은 불완전한 데이터 정리 및 실패 메시지 기록
        updateMessageInRedis(sessionId, msgId, "답변을 생성할 수 없습니다. (AI 서버 연결 실패)");

        log.info("[CHATBOT ERROR HANDLED] messageId={} 에러 처리 완료", msgId);
    }
}