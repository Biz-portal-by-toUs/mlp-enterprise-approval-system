package com.multi.mlpenterpriseapprovalsystem.subscription.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.payment.service.PortoneService;
import com.multi.mlpenterpriseapprovalsystem.subscription.dto.response.ResCompanySubscriptionDto;
import com.multi.mlpenterpriseapprovalsystem.subscription.dto.response.ResSubscriptionResultDto;
import com.multi.mlpenterpriseapprovalsystem.subscription.service.CompanySubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 회사구독정보 컨트롤러
 *
 * @author : 이지헌
 * @filename : CompanySubscriptionController
 * @since : 26. 1. 2. 금요일
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("api/v1")
public class CompanySubscriptionController {

    private final PortoneService portoneService;
    private final CompanySubscriptionService companySubscriptionService;

    /**
     * 내 회사의 현재 구독 정보 조회
     * GET /api/v1/company/subscription/me
     */
    @GetMapping("/company/subscription/me")
    public ResponseEntity<ResponseDto<ResCompanySubscriptionDto>> getMySubscription(@AuthenticationPrincipal CustomUser customUser) {
        ResCompanySubscriptionDto data = companySubscriptionService.getCurrentSubscription(customUser.getComId());
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "내 구독 정보 조회 성공", data));
    }


    /**
     * 요금제 구독 및 즉시 결제 요청
     * POST /api/v1/company/subscription/upgrade?subNo=2
     */
    @PostMapping("/company/subscription/upgrade")
    public ResponseEntity<ResponseDto<ResSubscriptionResultDto>> upgradeSubscription(
            @AuthenticationPrincipal CustomUser customUser,
            @RequestParam(name = "subNo") Long subNo) {

        String comId = customUser.getComId();
        ResSubscriptionResultDto result = portoneService.subscribeProPlan(comId, subNo);

        // 상태별 응답 메시지 결정
        String message = switch (result.getStatus()) {
            case "RESERVATION_COMPLETED" -> "요금제 변경 예약이 완료되었습니다. 다음 결제일부터 새로운 요금제가 적용됩니다.";
            case "DEPOSIT_ONLY" -> "보유하신 예치금으로 결제가 완료되었습니다.";
            case "PAYMENT_COMPLETED" -> String.format("%s원 결제가 완료되었습니다.", result.getPaidAmount().toPlainString());
            default -> "처리가 완료되었습니다.";
        };

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, message, result));
    }


    /**
     * 구독 해지 요청 (해지 예약)
     * PATCH /api/v1/company/subscription/cancel
     */
    @PatchMapping("/company/subscription/cancel")
    public ResponseEntity<ResponseDto<Void>> cancelSubscription(@AuthenticationPrincipal CustomUser customUser) {

        String comId = customUser.getComId();
        portoneService.cancelSubscription(comId);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "구독 해지 예약이 완료되었습니다. 만료일까지는 유료 기능 이용이 가능합니다.", null));
    }

    /**
     * 요금제 변경 예약 취소 (기존 구독 유지)
     * PATCH /api/v1/company/subscription/resume
     */
    @PatchMapping("/company/subscription/resume")
    public ResponseEntity<ResponseDto<Void>> resumeSubscription(@AuthenticationPrincipal CustomUser customUser) {
        String comId = customUser.getComId();
        portoneService.resumeSubscription(comId);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "요금제 변경 예약이 취소되었습니다. 기존 요금제가 유지됩니다.", null));
    }

    @GetMapping("/company/credit")
    public ResponseEntity<ResponseDto<BigDecimal>> getCredit(@AuthenticationPrincipal CustomUser customUser) {
        BigDecimal balance = companySubscriptionService.getCreditBalance(customUser.getComId());
        return ResponseEntity.ok(new ResponseDto<>(HttpStatus.OK, "예치금 조회 성공", balance));
    }

}
