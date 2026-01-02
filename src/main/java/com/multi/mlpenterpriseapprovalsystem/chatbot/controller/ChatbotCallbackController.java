package com.multi.mlpenterpriseapprovalsystem.chatbot.controller;

import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotCallbackDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * fastapi에서 콜백 (데이터) 받는 컨트롤러
 *
 * @author : 김승기
 * @filename : ChatbotCallbackController
 * @since : 2026. 1. 1. 목요일
 */

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotCallbackController {

    private final ChatbotService chatbotService;

    @Value("${internal.ai.callback-key:}")
    private String callbackKey;

    @PostMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestBody ReqChatbotCallbackDto request,
            @RequestHeader(value = "X-AI-CALLBACK-KEY", required = false) String key
    ) {
        // callbackKey 검증(회의 AI랑 동일)
        if (callbackKey != null && !callbackKey.isBlank()) {
            if (key == null || !callbackKey.equals(key)) {
                return ResponseEntity.status(401).build();
            }
        }

        chatbotService.handleCallback(request);
        return ResponseEntity.ok().build();
    }
}