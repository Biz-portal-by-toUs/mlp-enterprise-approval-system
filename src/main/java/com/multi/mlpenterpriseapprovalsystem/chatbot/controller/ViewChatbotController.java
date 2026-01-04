package com.multi.mlpenterpriseapprovalsystem.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 챗봇 뷰 컨트롤러
 *
 * @author : 김승기
 * @filename : ViewChatbotController
 * @since : 2026. 1. 2. 금요일
 */
@RequestMapping("/chatbot")
@Controller
public class ViewChatbotController {
    @GetMapping
    public String getlist(){
        return "chat/chatbot";
    }
}
