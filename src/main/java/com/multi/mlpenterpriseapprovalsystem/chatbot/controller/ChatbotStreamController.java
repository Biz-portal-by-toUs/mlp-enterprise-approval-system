package com.multi.mlpenterpriseapprovalsystem.chatbot.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.chatbot.service.ChatbotService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * sse 컨트롤러 (gpt처럼 답변이 한글자씩 실시간으로 화면에 나오기 위한 통로)
 *
 * @author : 김승기
 * @filename : ChatbotStreamController
 * @since : 2026. 1. 1. 목요일
 */

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotStreamController {

    private final ChatbotService chatbotService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @AuthenticationPrincipal CustomUser user,
            @RequestParam String sessionId,
            HttpServletResponse response
    ) {
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        return chatbotService.connectUserStream(user.getUsername(), sessionId);
    }
}