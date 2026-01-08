package com.multi.mlpenterpriseapprovalsystem.documentform.form.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 문서양식 화면(View) 라우팅 컨트롤러
 *
 * @author : 정종원
 * @filename : ViewDocumentFormController
 * @since : 2025-12-22
 */

@Controller
@RequestMapping("/form")
@RequiredArgsConstructor
public class ViewDocumentFormController {

    private static final String ROLE_EMPLOYEE  = "ROLE_EMPLOYEE";
    private static final String ROLE_SYS_ADMIN = "ROLE_SYS_ADMIN";
    private static final String ROLE_COM_ADMIN = "ROLE_COM_ADMIN";
    private static final String ROLE_SEC_ADMIN = "ROLE_SEC_ADMIN";
    private static final String ROLE_THR_ADMIN = "ROLE_THR_ADMIN";

    // 화면에서 쓸 "기능 플래그"를 백에서 계산해서 주입
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

        // ===== 규칙 =====
        // EMPLOYEE: 조회만
        // THR_ADMIN: 생성/수정/삭제요청/임시 가능
        // SEC/COM/SYS: THR_ADMIN + 승인/반려(상태변경) 가능

        boolean canView = isLogin;

        boolean canCreateForm    = isAnyAdmin;
        boolean canEditForm      = isAnyAdmin;
        boolean canDeleteRequest = isAnyAdmin; // 삭제 요청(soft)
        boolean canUseTemp       = isAnyAdmin;

        // 승인/반려/삭제승인/삭제반려
        boolean canApprove = isSysAdmin || isComAdmin || isSecAdmin;

        // list 화면에서 "선택 삭제" 표시 여부
        boolean canBulkDelete = canDeleteRequest;

        // pending 전용 perms
        boolean perm_canApproveForm = canApprove;
        boolean perm_canRejectForm  = canApprove;
        boolean perm_canViewReason  = (canApprove || isThrAdmin);

        // Boolean 세팅
        model.addAttribute("perm_isLogin", isLogin);
        model.addAttribute("perm_isEmployee", isEmployee);

        model.addAttribute("perm_canView", canView);
        model.addAttribute("perm_canCreateForm", canCreateForm);
        model.addAttribute("perm_canEditForm", canEditForm);
        model.addAttribute("perm_canDeleteRequest", canDeleteRequest);
        model.addAttribute("perm_canUseTemp", canUseTemp);
        model.addAttribute("perm_canApprove", canApprove);
        model.addAttribute("perm_canBulkDelete", canBulkDelete);

        // pending 전용
        model.addAttribute("perm_canApproveForm", perm_canApproveForm);
        model.addAttribute("perm_canRejectForm", perm_canRejectForm);
        model.addAttribute("perm_canViewReason", perm_canViewReason);

        // role flag
        model.addAttribute("perm_isThrAdmin", isThrAdmin);
        model.addAttribute("perm_isSysAdmin", isSysAdmin);
        model.addAttribute("perm_isComAdmin", isComAdmin);
        model.addAttribute("perm_isSecAdmin", isSecAdmin);

        // 기존 화면들이 perm_canEdit / perm_canDelete 로 읽는 케이스를 대비해서 "항상" 내려준다.
        model.addAttribute("perm_canEdit", canEditForm);
        model.addAttribute("perm_canDelete", canDeleteRequest);

        // 이미 쓰고 있지만, 템플릿이 perm_canApprove 로 읽으니 확실하게 보장
        model.addAttribute("perm_canApprove", canApprove);
    }

    private Set<String> extractAuthorities(CustomUser customUser) {
        if (customUser == null || customUser.getAuthorities() == null) return Set.of();
        return customUser.getAuthorities().stream()
                .filter(a -> a != null && a.getAuthority() != null && !a.getAuthority().isBlank())
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    // ===== routes =====

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN','EMPLOYEE')")
    @GetMapping("/forms")
    public String formList() {
        return "document-form/form-list";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN','EMPLOYEE')")
    @GetMapping("/{docfoNo}")
    public String formDetail(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/detail";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/new")
    public String createForm() {
        return "document-form/make-form";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/{docfoNo}/edit")
    public String updateForm(@PathVariable Long docfoNo, Model model) {
        model.addAttribute("docfoNo", docfoNo);
        return "document-form/update-form";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/pending")
    public String pendingList() {
        return "document-form/pending-form-list";
    }

    @GetMapping("/reject-reason")
    public String rejectReasonPopup(
            @RequestParam(required = false) Long docfoNo,
            @RequestParam(defaultValue = "view") String mode,
            Model model
    ) {
        model.addAttribute("docfoNo", docfoNo);
        model.addAttribute("mode", mode);
        return "document-form/form-reject-reason";
    }

    @PreAuthorize("hasAnyRole('SYS_ADMIN','COM_ADMIN','SEC_ADMIN','THR_ADMIN')")
    @GetMapping("/temp")
    public String tempList() {
        return "document-form/temp-form-list";
    }
}