package com.multi.mlpenterpriseapprovalsystem.payment.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.res.ResPaymentHistoryDto;
import com.multi.mlpenterpriseapprovalsystem.payment.service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제내역 컨트롤러
 *
 * @author : 이지헌
 * @filename : PaymentHistoryController
 * @since : 26. 1. 8. 목요일
 */

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PaymentHistoryController {

    private final PaymentHistoryService paymentHistoryService;

    @GetMapping("/company/payment-history")
    public ResponseEntity<ResponseDto<Page<ResPaymentHistoryDto>>> getMyPaymentHistory(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        Pageable pageable = PageRequest.of(page, 10); // 10개씩 페이징
        Page<ResPaymentHistoryDto> data = paymentHistoryService.getPaymentHistories(customUser.getComId(), pageable);

        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "결제 내역 조회 성공", data));
    }


}
