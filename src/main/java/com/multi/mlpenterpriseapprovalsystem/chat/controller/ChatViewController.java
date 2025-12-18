package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ChatViewController
 * @since : 2025. 12. 18. 목요일
 */
@Controller
@RequestMapping("/chat")
public class ChatViewController {

    @GetMapping("/panel")
    public String chatPanel() {
        return "chat/panel";
    }

    @GetMapping("/rooms")
    public String chatRooms() {
        return "chat/rooms";
    }

    @GetMapping("/employees")
    public String employees() {
        return "chat/employees";
    }
}