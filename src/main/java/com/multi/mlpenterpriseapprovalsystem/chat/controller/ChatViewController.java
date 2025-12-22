package com.multi.mlpenterpriseapprovalsystem.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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


    // 메인(사원탭/채팅탭)
    @GetMapping
    public String chatHome() {
        return "chat/chat-main"; // 네가 가진 chat.html(템플릿) 이름에 맞추기
    }

    // 방 전용(새 창으로 열기)
    @GetMapping("/room/{roomNo}")
    public String room(@PathVariable(name = "roomNo") Long roomNo, Model model) {
        model.addAttribute("roomNo", roomNo);
        return "chat/chat-room"; // room.html 하나 만들거나, chat.html을 재사용해도 됨(쿼리로 구분)
    }

    // 단톡 생성 페이지
    @GetMapping("/new")
    public String newRoom() {
        return "chat/chat-create";
    }

}