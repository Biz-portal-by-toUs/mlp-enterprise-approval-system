package com.multi.mlpenterpriseapprovalsystem.chat.domain;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 채팅 메세지 컬렉션 (MongoDB)
 *
 * @author : 김승기
 * @filename : ChatMessage
 * @since : 2025. 12. 16. 화요일
 */
@Document(collection = "chat_messages")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    private String id;

    private Long roomNo;

    private String senderEmpId;
    private String senderName;

    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, IMAGE, SYSTEM

    @CreatedDate
    private LocalDateTime createdAt;
}