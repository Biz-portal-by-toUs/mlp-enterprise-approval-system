package com.multi.mlpenterpriseapprovalsystem.organization.positions.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.repository.PositionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 직급 조회 페이지 반환 컨트롤러
 *
 * @author : 권지영
 * @filename : ViewPositionsController
 * @since : 2025. 12. 22. 월요일
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/positions")
public class ViewPositionsController {

    private final PositionsRepository positionsRepository;

    @GetMapping("/list")
    public String listPage(@AuthenticationPrincipal CustomUser user, Model model) {
        // 필요하면 화면에 표시할 값 전달(지금 템플릿은 표시 안 하고 있음)
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");
        return "positions/list";
    }

    @GetMapping("/create")
    public String createPage(@AuthenticationPrincipal CustomUser user, Model model) {
        // 필요하면 화면에 표시할 값 전달(지금 템플릿은 표시 안 하고 있음)
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");
        return "positions/create";
    }

    @GetMapping("/{posNo}/edit")
    public String editPage(
            @PathVariable(name = "posNo") Long posNo,
            @AuthenticationPrincipal CustomUser user, // 로그인한 유저 정보를 바로 가져옴
            Model model
    ) {
        // 1. 부서 정보 조회
        Positions positions = positionsRepository.findById(posNo)
                .orElseThrow(() -> new CustomException(ErrorCode.POSITIONS_NOT_FOUND));

        // 2. 모델에 직급 정보 담기
        model.addAttribute("posName", positions.getPosName());
        model.addAttribute("posNo", posNo);
        model.addAttribute("posOrder", positions.getPosOrder());

        // 3. 로그인한 유저 정보가 필요하다면?
        model.addAttribute("username", user != null ? user.getUsername() : "");
        model.addAttribute("comId", user != null ? user.getComId() : "");

        return "positions/update";
    }
}
