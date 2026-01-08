package com.multi.mlpenterpriseapprovalsystem.payment.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.req.ReqVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.res.ResPaymentMethodDto;
import com.multi.mlpenterpriseapprovalsystem.payment.service.PaymentMethodService;
import com.multi.mlpenterpriseapprovalsystem.payment.service.PortoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final PortoneService portoneService;

    /**
     * 등록된 모든 결제 수단 조회 API
     * GET /api/v1/company/payment-method
     */
    @GetMapping("/company/payment-method")
    public ResponseEntity<ResponseDto<List<ResPaymentMethodDto>>> getPaymentMethods(
            @AuthenticationPrincipal CustomUser customUser) {

        List<ResPaymentMethodDto> paymentMethods = paymentMethodService.getPaymentMethods(customUser.getComId());

        return ResponseEntity.ok(
                new ResponseDto<>(HttpStatus.OK, "결제 수단 목록 조회 성공", paymentMethods)
        );
    }

    // 카드 등록 (빌링키 발급 결과 저장)
    @PostMapping("/company/payment-method")
    public ResponseEntity<ResponseDto<Void>> registerPaymentMethod(@RequestBody ReqVerifyDto reqVerifyDto,
                                                                   @AuthenticationPrincipal CustomUser customUser) {
        log.info("[/api/v1/company/payment-method] VerifyRequestDto = " + reqVerifyDto.toString());

        String comId = customUser.getComId();

        // 카드 등록
        paymentMethodService.registerCard(comId, reqVerifyDto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ResponseDto<>(HttpStatus.CREATED, "카드 등록 성공", null));

    }

    // 대표 결제 수단 변경
    @PatchMapping("/company/payment-method/{paymNo}/representative")
    public ResponseEntity<ResponseDto<Void>> updateRepresentativeCard(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "paymNo") Long paymNo) {

        paymentMethodService.updateRepresentativeCard(customUser.getComId(), paymNo);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "대표 결제 수단이 변경되었습니다.", null));
    }

    // 결제 수단 삭제
    @DeleteMapping("/company/payment-method/{paymNo}")
    public ResponseEntity<ResponseDto<Void>> deleteCard(
            @AuthenticationPrincipal CustomUser customUser,
            @PathVariable(name = "paymNo") Long paymNo) {

        paymentMethodService.deleteCard(customUser.getComId(), paymNo);
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "결제 수단이 삭제되었습니다.", null));
    }


}
