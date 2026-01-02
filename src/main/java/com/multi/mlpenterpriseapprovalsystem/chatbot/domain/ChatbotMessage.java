package com.multi.mlpenterpriseapprovalsystem.chatbot.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 챗봇 메세지 저장 Document
 *
 * @author : 김승기
 * @filename : ChatbotMessage
 * @since : 2026. 1. 1. 목요일
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatbot_messages")
@CompoundIndex(name="idx_emp_created_id", def="{'empId':1,'createdAt':-1,'_id':-1}")
public class ChatbotMessage {

    @Id
    private String id;              // ObjectId(String)

    private String empId;           // "한 방"의 소유자(유저 키)
    private MessageRole role;       // USER / ASSISTANT
    private MessageStatus status;   // STREAMING / DONE / ERROR

    private String content;         // 메시지 내용(답변은 chunk 누적)
    private String errorMessage;    // ERROR일 때

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatbotMessage user(String empId, String question) {
        LocalDateTime now = LocalDateTime.now();
        return ChatbotMessage.builder()
                .empId(empId)
                .role(MessageRole.USER)
                .status(MessageStatus.DONE)
                .content(question)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public static ChatbotMessage assistantStreaming(String empId) {
        LocalDateTime now = LocalDateTime.now();
        return ChatbotMessage.builder()
                .empId(empId)
                .role(MessageRole.ASSISTANT)
                .status(MessageStatus.STREAMING)
                .content("")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void appendChunk(String chunk) {
        if (chunk == null || chunk.isBlank()) return;
        if (this.content == null) this.content = "";
        this.content += chunk;
        this.updatedAt = LocalDateTime.now();
    }

    public void markDone() {
        this.status = MessageStatus.DONE;
        this.updatedAt = LocalDateTime.now();
    }

    public void markError(String msg) {
        this.status = MessageStatus.ERROR;
        this.errorMessage = msg;
        this.updatedAt = LocalDateTime.now();
    }
}