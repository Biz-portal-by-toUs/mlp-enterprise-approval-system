package com.multi.mlpenterpriseapprovalsystem.documentform.attachment.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.*;
import com.multi.mlpenterpriseapprovalsystem.common.exception.*;
import lombok.*;
import org.springframework.security.core.*;
import org.springframework.security.core.annotation.*;
import org.springframework.stereotype.*;
import org.springframework.ui.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.*;

/**
 * Please explain the class!!!
 *
 * @author : 정종원
 * @filename : ViewAttachBoxController
 * @since : 2026-01-10 토요일
 */

@Controller
@RequestMapping("/attach")
@RequiredArgsConstructor
public class ViewAttachBoxController {

    private static final String ROLE_EMPLOYEE  = "ROLE_EMPLOYEE";
    private static final String ROLE_SYS_ADMIN = "ROLE_SYS_ADMIN";
    private static final String ROLE_COM_ADMIN = "ROLE_COM_ADMIN";
    private static final String ROLE_SEC_ADMIN = "ROLE_SEC_ADMIN";
    private static final String ROLE_THR_ADMIN = "ROLE_THR_ADMIN";

    // View에서 사용할 권한/기능 플래그 주입
    @ModelAttribute
    public void addPermissionsToModel(
            @AuthenticationPrincipal CustomUser customUser,
            Model model
    ) {
        Set<String> auths = extractAuthorities(customUser);

        boolean isLogin = (customUser != null);

        boolean isSysAdmin = auths.contains(ROLE_SYS_ADMIN);
        boolean isComAdmin = auths.contains(ROLE_COM_ADMIN);
        boolean isSecAdmin = auths.contains(ROLE_SEC_ADMIN);
        boolean isThrAdmin = auths.contains(ROLE_THR_ADMIN);

        boolean isAnyAdmin = isSysAdmin || isComAdmin || isSecAdmin || isThrAdmin;

        boolean isEmployee = !isAnyAdmin && auths.contains(ROLE_EMPLOYEE);

        boolean canView = isLogin;
        boolean canCreateAttach = isAnyAdmin;
        boolean canDeleteAttach = isAnyAdmin;

        model.addAttribute("perm_isLogin", isLogin);
        model.addAttribute("perm_isEmployee", isEmployee);

        model.addAttribute("perm_canView", canView);
        model.addAttribute("perm_canCreateAttach", canCreateAttach);
        model.addAttribute("perm_canDeleteAttach", canDeleteAttach);

        // form-list.html 스타일과 최대한 맞추기 위한 alias
        model.addAttribute("perm_canCreate", canCreateAttach);
        model.addAttribute("perm_canDelete", canDeleteAttach);

        // html classappend 용
        model.addAttribute("perm_isAnyAdmin", isAnyAdmin);
        model.addAttribute("perm_isThrAdmin", isThrAdmin);
        model.addAttribute("perm_isSysAdmin", isSysAdmin);
        model.addAttribute("perm_isComAdmin", isComAdmin);
        model.addAttribute("perm_isSecAdmin", isSecAdmin);
    }

    private Set<String> extractAuthorities(CustomUser customUser) {
        if (customUser == null || customUser.getAuthorities() == null) return Set.of();
        return customUser.getAuthorities().stream()
                .filter(a -> a != null && a.getAuthority() != null && !a.getAuthority().isBlank())
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // 첨부 목록
    @GetMapping("/attaches")
    public String attachList(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return "document-form/attach-list";
    }

    // 첨부 상세
    @GetMapping("/{attachNo}")
    public String attachDetail(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable Long attachNo,
            Model model
    ) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        if (attachNo == null || attachNo <= 0) throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);

        model.addAttribute("attachNo", attachNo); // 상세 화면에서 JS가 쓰면 편함
        return "document-form/attach-detail";
    }

    // 첨부 추가
    @GetMapping("/create")
    public String attachCreate(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) throw new CustomException(ErrorCode.UNAUTHORIZED);
        return "document-form/attach-add";
    }
}