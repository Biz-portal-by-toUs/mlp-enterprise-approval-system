package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatMessageSendDto;
import com.multi.mlpenterpriseapprovalsystem.chat.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 채팅 웹소켓 컨트롤러 (레디스 Pub/sub)
 *
 * @author : 김승기
 * @filename : WebSocketController
 * @since : 2025. 12. 17. 수요일
 */

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatSocketController {
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat/message")
    public void send(ReqChatMessageSendDto req, Principal principal) {
        String empId = principal.getName();
        chatMessageService.sendMessage(req, empId);
    }

}
