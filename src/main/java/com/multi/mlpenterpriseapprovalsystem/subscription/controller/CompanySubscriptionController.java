package com.multi.mlpenterpriseapprovalsystem.subscription.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.payment.service.PortoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 요금제 구독 및 즉시 결제 요청
     * POST /api/v1/company/subscription/upgrade?subNo=2
     */
    @PostMapping("/company/subscription/upgrade")
    public ResponseEntity<ResponseDto<Void>> upgradeSubscription(@AuthenticationPrincipal CustomUser customUser,
                                                                 @RequestParam(name = "subNo") Long subNo) {

        String comId = customUser.getComId();
        portoneService.subscribeProPlan(comId, subNo);

        return ResponseEntity
                .ok()
                .body(new ResponseDto<>(HttpStatus.OK, "구독 업그레이드 및 결제 성공", null));
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
}
