package com.multi.mlpenterpriseapprovalsystem.payment.controller;

import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.ReqVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.payment.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 수단 처리 컨트롤러
 *
 * @author : 이지헌
 * @filename : PaymentController
 * @since : 25. 12. 17. 수요일
 */
@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    // 카드 등록 (빌링키 발급 결과 저장)
    @PostMapping("/company/payment-method")
    public ResponseEntity<ResponseDto<Void>> registerPaymentMethod(@RequestBody ReqVerifyDto reqVerifyDto) {
        log.info("[/api/v1/company/payment-method] VerifyRequestDto = " + reqVerifyDto.toString());

        // AccessToken에서 회사코드 꺼냈다고 가정
        String comId = "A01";

        // 카드 등록
        paymentMethodService.registerCard(comId, reqVerifyDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "카드 등록 성공", null));

    }
}
