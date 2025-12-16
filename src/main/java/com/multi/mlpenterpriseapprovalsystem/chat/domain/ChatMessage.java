package com.multi.mlpenterpriseapprovalsystem.chat.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@Document(collection = "chat_message")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    private String id;

    private String roomId;

    private Long senderId;
    private String senderName;

    private String content;
    private MessageType type; // TEXT, IMAGE, SYSTEM

    private LocalDateTime createdAt;
}