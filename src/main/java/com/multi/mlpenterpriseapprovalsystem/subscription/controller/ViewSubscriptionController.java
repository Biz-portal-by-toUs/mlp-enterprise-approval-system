package com.multi.mlpenterpriseapprovalsystem.subscription.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 요금제 화면용 컨트롤러
 *
 * @author : 이지헌
 * @filename : ViewSubscriptionController
 * @since : 25. 12. 16. 화요일
 */

@Controller
public class ViewSubscriptionController {

    @GetMapping("/company/subscriptions")
    public String viewSubscriptions() {
        return "subscription/list";
    }

    @GetMapping("/subscriptions/intro")
    public String viewSubscriptionsIntro() {
        return "subscription/intro";
    }



}
