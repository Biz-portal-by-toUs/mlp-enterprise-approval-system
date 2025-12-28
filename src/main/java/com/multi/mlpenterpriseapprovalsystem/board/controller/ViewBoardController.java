package com.multi.mlpenterpriseapprovalsystem.board.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : FrontNoticeController
 * @since : 2025-12-17 수요일
 */
@Controller
@RequestMapping("/board")
public class ViewBoardController {

    @GetMapping("/list")
    public String boardList() {

        return "board/board_list";
    }

    // 상세 조회
    @GetMapping("/{boardNo}")
    public String placeDetail(@PathVariable("boardNo") int boardNo, Model model) {
        model.addAttribute("boardNo", boardNo);
        return "board/detail";

    }

}
