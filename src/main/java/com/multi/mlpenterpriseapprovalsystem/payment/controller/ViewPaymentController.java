package com.multi.mlpenterpriseapprovalsystem.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 결제 화면용 컨트롤러
 *
 * @author : 이지헌
 * @filename : ViewPaymentMethodController
 * @since : 25. 12. 17. 수요일
 */

@Controller
public class ViewPaymentController {

    @GetMapping("/payment-methods/register")
    public String viewPaymentMethodRegister(Model model) {
        return "payment/method/register";
    }

    @GetMapping("/company/payment-methods")
    public String viewPaymentMethods(){
        return "payment/method/list";
    }

    @GetMapping("/company/payment-historys")
    public String viewPaymentHistorys(Model model) {
        return "payment/history/list";
    }
}
