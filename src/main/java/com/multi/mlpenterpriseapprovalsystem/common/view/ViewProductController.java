package com.multi.mlpenterpriseapprovalsystem.common.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 *  제품 소개 뷰 컨트롤러
 * @author : 김승기
 * @filename : ProductController
 * @since : 2026. 1. 18. 일요일
 */
@Controller
public class ViewProductController {
    @GetMapping("/products/intro")
    public String productIntro() {
        return "products/intro";
    }
}
