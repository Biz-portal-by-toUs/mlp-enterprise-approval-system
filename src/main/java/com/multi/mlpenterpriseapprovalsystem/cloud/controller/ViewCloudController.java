package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.enums.FolderScope;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * 클라우드(공유/개인 파일함) 화면을 띄워주는 뷰 컨트롤러
 *
 * @author : 송현님
 * @filename : ViewFolderController
 * @since : 2025-12-29 오후 5:37 월요일
 */
@Controller
public class ViewCloudController {

    private static final Set<String> ADMIN_AUTHORITIES = Set.of(
            "SYS_ADMIN", "COM_ADMIN", "SEC_ADMIN", "THR_ADMIN",
            "ROLE_SYS_ADMIN", "ROLE_COM_ADMIN", "ROLE_SEC_ADMIN", "ROLE_THR_ADMIN"
    );

    private boolean isAdmin(CustomUser user) {
        return user != null
                && user.getAuthorities() != null
                && user.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(ADMIN_AUTHORITIES::contains);
    }

    /** 기본 진입: 공유함으로 */
    @GetMapping("/cloud")
    public String root() {
        return "redirect:/cloud/dept";
    }

    /** 공유함 */
    @GetMapping("/cloud/dept")
    public String dept(Model model) {
        model.addAttribute("active", "dept"); // sidebar active 표시용
        model.addAttribute("scope", "dept");  // JS 초기 scope용
        return "cloud/cloud";
    }

    /** 개인함 */
    @GetMapping("/cloud/prvt")
    public String prvt(Model model) {
        model.addAttribute("active", "prvt");
        model.addAttribute("scope", "prvt");
        return "cloud/cloud";
    }


    /** ✅ 휴지통 */
    @GetMapping("/cloud/trash")
    public String trash(
            @RequestParam(defaultValue = "DEPT", name = "scope") FolderScope scope,
            Model model,
            @AuthenticationPrincipal CustomUser user
    ) {
        String tabActive = (scope == FolderScope.PRVT) ? "prvt" : "dept";

        model.addAttribute("active", tabActive);
        model.addAttribute("scope", tabActive);
        model.addAttribute("trashScope", scope);

        model.addAttribute("isTrash", true);
        model.addAttribute("activeAdminLog", false);
        model.addAttribute("hideTreeTools", true);

        // ✅ 사이드바 토글 숨기고 뒤로가기 노출
        model.addAttribute("showBackButton", true);
        model.addAttribute("backUrl", "/cloud/" + tabActive); // dept -> /cloud/dept, prvt -> /cloud/prvt

        // ✅ 관리자 삭제 로그 버튼: "공유함 휴지통(DEPT)"에서만 + 관리자만
        boolean showAdminLogButton = isAdmin(user) && scope == FolderScope.DEPT;
        model.addAttribute("showAdminLogButton", showAdminLogButton);

        return "cloud/trash";
    }

    @GetMapping("/cloud/trash/admin-log")
    public String adminTrashLog(
            @RequestParam(defaultValue = "DEPT", name = "scope") FolderScope scope,
            Model model,
            @AuthenticationPrincipal CustomUser user
    ) {
        if (!isAdmin(user)) {
            return "redirect:/cloud/trash?scope=" + scope.name();
        }

        String tabActive = (scope == FolderScope.PRVT) ? "prvt" : "dept";

        model.addAttribute("active", tabActive);
        model.addAttribute("scope", tabActive);
        model.addAttribute("trashScope", scope);

        model.addAttribute("isTrash", false);
        model.addAttribute("activeAdminLog", true);
        model.addAttribute("hideTreeTools", true);

        // ✅ 관리자로그 화면도 "휴지통 진입 상태"로 보고 토글 숨김 + 뒤로가기
        model.addAttribute("showBackButton", true);
        model.addAttribute("backUrl", "/cloud/" + tabActive);

        // ✅ 관리자 삭제 로그 버튼은 공유함(DEPT)에서만
        model.addAttribute("showAdminLogButton", scope == FolderScope.DEPT);

        return "cloud/admin-log";
    }
}

