package com.multi.mlpenterpriseapprovalsystem.document_form.form.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 문서양식 화면(View) 라우팅 컨트롤러
 *
 * @author : 정종원
 * @filename : ViewDocumentFormController
 * @since : 2025-12-22 월요일
 */
@Controller
@RequestMapping("/form")
@RequiredArgsConstructor
public class ViewDocumentFormController {

    // 화면에서 쓸 "기능 플래그"를 백에서 계산해서 주입
    @ModelAttribute
    public void addPermissionsToModel(
            @AuthenticationPrincipal CustomUser customUser,
            Model model
    ) {
        Set<String> auths = new HashSet<>();
        if (customUser != null && customUser.getAuthorities() != null) {
            customUser.getAuthorities().forEach(a -> {
                if (a != null && a.getAuthority() != null) auths.add(a.getAuthority());
            });
        }

        boolean isLogin = (customUser != null);

        boolean isEmployee = auths.contains("ROLE_EMPLOYEE");
        boolean isSysAdmin = auths.contains("ROLE_SYS_ADMIN");
        boolean isComAdmin = auths.contains("ROLE_COM_ADMIN");
        boolean isSecAdmin = auths.contains("ROLE_SEC_ADMIN");
        boolean isThrAdmin = auths.contains("ROLE_THR_ADMIN");

        // ===== 규칙 =====
        // EMPLOYEE: 조회만
        // THR_ADMIN: 생성/수정/삭제요청/임시 가능
        // SEC/COM/SYS: THR_ADMIN + 승인/반려(상태변경) 가능

        boolean canView = isLogin;

        boolean canCreateForm     = isSysAdmin || isComAdmin || isSecAdmin || isThrAdmin;
        boolean canEditForm       = isSysAdmin || isComAdmin || isSecAdmin || isThrAdmin;
        boolean canDeleteRequest  = isSysAdmin || isComAdmin || isSecAdmin || isThrAdmin; // 삭제 요청(soft)
        boolean canUseTemp        = isSysAdmin || isComAdmin || isSecAdmin || isThrAdmin;

        // 승인/반려/삭제승인/삭제반려
        boolean canApprove = isSysAdmin || isComAdmin || isSecAdmin;

        // list 화면에서 "선택 삭제" 표시 여부
        boolean canBulkDelete = canDeleteRequest;

        // ===== pending 전용 perms (프론트 data-*) =====
        boolean perm_canApproveForm = canApprove;         // 승인 버튼 렌더 여부
        boolean perm_canRejectForm  = canApprove;         // 반려 버튼 렌더 여부
        boolean perm_canViewReason  = (canApprove || isThrAdmin); // 사유 보기 가능(THR_ADMIN 포함)

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

        // role flag도 필요하면 같이
        model.addAttribute("perm_isThrAdmin", isThrAdmin);
        model.addAttribute("perm_isSysAdmin", isSysAdmin);
        model.addAttribute("perm_isComAdmin", isComAdmin);
        model.addAttribute("perm_isSecAdmin", isSecAdmin);
    }

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