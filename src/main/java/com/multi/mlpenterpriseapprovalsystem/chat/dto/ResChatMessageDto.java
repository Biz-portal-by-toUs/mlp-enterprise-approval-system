package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.ChatMessage;
import com.multi.mlpenterpriseapprovalsystem.chat.domain.MessageType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅 메세지 resDto
 *
 * @author : 김승기
 * @filename : ResChatMessage
 * @since : 2025. 12. 17. 수요일
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResChatMessageDto {

    private String messageId;
    private Long roomNo;
    private String senderEmpId;
    private String senderName;
    private String content;
    private MessageType type;
    private String createdAt;

    public static ResChatMessageDto from(ChatMessage msg) {
        return new ResChatMessageDto(
                msg.getId(),
                msg.getRoomNo(),
                msg.getSenderEmpId(),
                msg.getSenderName(),
                msg.getContent(),
                msg.getType(),
                msg.getCreatedAt() == null ? null : msg.getCreatedAt().toString()
        );
    }
}