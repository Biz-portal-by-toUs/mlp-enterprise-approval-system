package com.multi.mlpenterpriseapprovalsystem.subscription.controller;

import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.subscription.dto.response.ResSubscriptionDto;
import com.multi.mlpenterpriseapprovalsystem.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 요금제 정보를 RestApi를 통해 관리하는 RestController
 *
 * @author : 이지헌
 * @filename : SubscriptionController
 * @since : 25. 12. 16. 화요일
 */

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 전체 요금제 조회
    @GetMapping("/subscriptions")
    public ResponseEntity<ResponseDto> getSubscriptions() {

        List<ResSubscriptionDto> resSubscriptionDtos = subscriptionService.getSubscriptions();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(new ResponseDto(HttpStatus.OK, "전체 요금제 조회 성공", resSubscriptionDtos));
    }

}
