package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 채팅 위젯 뷰 컨트롤러
 *
 * @author : 김승기
 * @filename : ChatViewController
 * @since : 2025. 12. 18. 목요일
 */
@Controller
@RequestMapping("/chat")
public class ChatViewController {


    @GetMapping("/chat")
    public String chatPage() {
        return "chat/chat";
    }

}