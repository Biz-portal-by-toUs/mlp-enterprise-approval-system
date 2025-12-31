package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 클라우드(공유/개인 파일함) 화면을 띄워주는 뷰 컨트롤러
 *
 * @author : 송현님
 * @filename : ViewFolderController
 * @since : 2025-12-29 오후 5:37 월요일
 */
@Controller
public class ViewCloudController {

    @GetMapping("/cloud")
    public String page() {
        return "cloud/cloud";
    }
}
