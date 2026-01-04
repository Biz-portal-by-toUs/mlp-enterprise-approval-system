package com.multi.mlpenterpriseapprovalsystem.board.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.board.dto.BoardCatDto;
import com.multi.mlpenterpriseapprovalsystem.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : FrontNoticeController
 * @since : 2025-12-17 수요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class ViewBoardController {

   private final BoardService boardService;

    @GetMapping("/Attachment-board/{boardNo}")
    public String noticeAttach(@PathVariable("boardNo") Long boardNo, Model model) {
        model.addAttribute("boardNo", boardNo);
        return "board/Attachment-board";
    }

    @GetMapping("/list")
    public String boardList() {
        return "board/board-list";
    }

    // 상세 조회
    @GetMapping("/{boardNo}")
    public String boardDetail(@PathVariable("boardNo") int boardNo, Model model,
                              @AuthenticationPrincipal CustomUser user) {
        model.addAttribute("boardNo", boardNo);
        model.addAttribute("loginEmpId", user.getUsername());
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("COM_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
        return "board/board-detail";
    }

    // 📍 리뷰 등록 폼 페이지
    @GetMapping("/form")
    public String boardFormPage(Model model) {
        List<BoardCatDto> cateList = boardService.getAllCategory();
        model.addAttribute("categories", cateList);
        return "board/board-form";
    }

}
