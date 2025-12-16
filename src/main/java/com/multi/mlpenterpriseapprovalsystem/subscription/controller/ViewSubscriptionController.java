package com.multi.mlpenterpriseapprovalsystem.subscription.controller;

import com.multi.mlpenterpriseapprovalsystem.subscription.dto.response.ResSubscriptionDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Please explain the class!!!
 *
 * @author : 이지헌
 * @filename : ViewSubscriptionController
 * @since : 25. 12. 16. 화요일
 */

@Controller
public class ViewSubscriptionController {

    @GetMapping("/subscriptions")
    public String viewSubscriptions(ResSubscriptionDto resSubscriptionDto) {
        return "subscription/subscriptions";
    }

}
