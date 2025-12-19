package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ReqChatMessageSendDto;
import com.multi.mlpenterpriseapprovalsystem.chat.dto.ResChatMessageDto;
import com.multi.mlpenterpriseapprovalsystem.chat.service.ChatMessageService;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 send 및 채팅 메세지 조회 컨트롤러
 *
 * @author : 김승기
 * @filename : ChatMessageController
 * @since : 2025. 12. 18. 목요일
 */
@RestController
@RequestMapping("/api/v1/chat/messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ResponseDto<ResChatMessageDto>> sendMessage(
            @RequestBody ReqChatMessageSendDto request,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        ResChatMessageDto result = chatMessageService.sendMessage(request, empId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(
                        HttpStatus.CREATED,
                        "메시지 전송 성공",
                        result
                ));
    }


    @GetMapping("/{roomNo}")
    public ResponseEntity<ResponseDto<List<ResChatMessageDto>>> getMessages(
            @PathVariable Long roomNo,
            @RequestParam(required = false) LocalDateTime cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUser user
    ) {
        String empId = user.getUsername();
        List<ResChatMessageDto> messages =
                chatMessageService.getMessages(roomNo, cursor, size, empId);

        return ResponseEntity.ok(
                new ResponseDto<>(
                        HttpStatus.OK,
                        "메시지 조회 성공",
                        messages
                )
        );
    }
}