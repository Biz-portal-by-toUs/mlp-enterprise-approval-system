package com.multi.mlpenterpriseapprovalsystem.chat.dto;

import com.multi.mlpenterpriseapprovalsystem.chat.domain.MessageType;
import lombok.Getter;

/**
 * 채팅 메세지 reqDto
 *
 * @author : 김승기
 * @filename : ReqChatMessageSendDto
 * @since : 2025. 12. 18. 목요일
 */
@Getter
public class ReqChatMessageSendDto {

    private Long roomNo;
    private String content;
    private MessageType type; // TEXT, IMAGE, SYSTEM
}