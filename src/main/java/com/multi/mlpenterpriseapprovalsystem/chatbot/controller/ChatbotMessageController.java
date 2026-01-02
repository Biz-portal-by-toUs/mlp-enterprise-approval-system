package com.multi.mlpenterpriseapprovalsystem.chatbot.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.chatbot.domain.ChatbotMessage;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ReqChatbotMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.dto.ResChatbotMessageCreatedDto;
import com.multi.mlpenterpriseapprovalsystem.chatbot.service.ChatbotService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.client.ChatbotAiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        String comId= user.getComId();

        ResChatbotMessageCreatedDto created =
                chatbotService.createQuestionAndPrepareAnswer(request, empId);

        chatbotAiClient.requestChatbotAnswer(
                created.getAssistantMessageId(),
                empId,
                comId,
                request.getQuestion()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResponseDto<>(
                        HttpStatus.CREATED,
                        "챗봇 질문 등록 성공",
                        created
                ));
    }

    @GetMapping("/messages")
    public ResponseEntity<ResponseDto<List<ChatbotMessage>>> messages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeAt,
            @RequestParam(required = false) String beforeId,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        List<ChatbotMessage> list = chatbotService.getMessages(empId, limit, beforeAt, beforeId);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "메시지 조회 성공", list));
    }
}
