package com.multi.mlpenterpriseapprovalsystem.chatbot.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ResChatbotMessageCreatedDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.service.ChatbotService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.client.ChatbotAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 챗봇 메세지 컨트롤러 (발송 및 저장)
 *
 * @author : 김승기
 * @filename : ChatbotMessageController
 * @since : 2026. 1. 1. 목요일
 */
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotMessageController {
    private final ChatbotService chatbotService;
    private final ChatbotAiClient chatbotAiClient;

    @PostMapping("/messages")
    public ResponseEntity<ResponseDto<ResChatbotMessageCreatedDto>> sendQuestion(
            @RequestBody ReqChatbotMessageDto request,
            @RequestHeader("X-Session-Id") String sessionId,
            @AuthenticationPrincipal CustomUser user
    ) {
        ResChatbotMessageCreatedDto created = chatbotService.processQuestion(
                request,
                user.getUsername(),
                user.getComId(),
                sessionId
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "질문 등록 성공", created));
    }

}
