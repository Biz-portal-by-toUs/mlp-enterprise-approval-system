package com.multi.mlpenterpriseapprovalsystem.payment.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentHistory;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.res.ResPortonePaymentInfoDto;
import com.multi.mlpenterpriseapprovalsystem.payment.repository.PaymentHistoryRepository;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.CompanySubscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.enums.SubStatus;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.CompanySubscriptionRepository;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 포트원서버에 API요청하는 파일
 *
 * accessToken발급 API:
 * https://api.iamport.kr/users/getToken
 *
 * 결제ID로 결제 정보 조회:
 * https://api.iamport.kr/users/payments/{결제ID}
 *
 * @author : 이지헌
 * @filename : PortoneService
 * @since : 25. 12. 17. 수요일
 */

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PortoneService {
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final SubscriptionRepository subscriptionRepository; // 요금제 정보
    private final PaymentHistoryRepository paymentHistoryRepository;


    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.iamport.kr")
            .build();

    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    // 포트원에 접근하기 위한 액세스 토큰 발급
    private String getPortoneAccessToken() {

        log.info("API Key: {}", apiKey);
        log.info("API Secret: {}", apiSecret);

        // 엑세스 토큰 발급 api
        Map<String, Object> response = webClient
                .post()
                .uri("/users/getToken")
                .bodyValue(Map.of("imp_key", apiKey, "imp_secret", apiSecret))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Integer code = (Integer) response.get("code");
        if (code != 0) {
            throw new CustomException(ErrorCode.PORTONE_TOKEN_ERROR);
        }

        Map<String, Object> responseBody = (Map<String, Object>) response.get("response");

        return (String) responseBody.get("access_token");
    }

    // 결제 정보 단건 조회
    public ResPortonePaymentInfoDto getPaymentInfo(String impUid) {
        String portoneAccessToken = getPortoneAccessToken();

        // 결제ID에 해당하는 결제정보 조회
        Map<String, Object> rootNode = webClient
                .get()
                .uri("/payments/" + impUid)
                .header("Authorization", "Bearer " + portoneAccessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        Integer code = (Integer) rootNode.get("code");
        if (code != 0) {
            throw new CustomException(ErrorCode.PORTONE_PAYMENT_LOOKUP_ERROR);
        }

        Map<String, Object> res = (Map<String, Object>) rootNode.get("response");

        ResPortonePaymentInfoDto dto = new ResPortonePaymentInfoDto();
        dto.setStatus((String) res.get("status"));
        dto.setAmount((Integer) res.get("amount"));
        dto.setImpUid((String) res.get("imp_uid"));
        dto.setCustomerUid((String) res.get("customer_uid"));
        dto.setPayMethod((String) res.get("pay_method"));
        dto.setCardName((String) res.get("card_name"));
        dto.setCardNumber((String) res.get("card_number"));

        return dto;
    }

    // 즉시 결제 요청
    public void subscribeProPlan(String comId, Long subNo) {
        // 1. 필요한 정보 조회 (회사, 선택한 요금제, 등록된 카드)
        CompanySubscription companySub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        Subscription plan = subscriptionRepository.findById(subNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

        PaymentMethod paymentMethod = companySub.getPaymentMethod();
        if (paymentMethod == null || !paymentMethod.getActive()) {
            throw new CustomException(ErrorCode.PAYMENT_METHOD_NOT_FOUND); // "유효한 결제 수단이 없습니다."
        }

        // 2. 즉시 결제 요청 (주문번호는 회사번호+시간 등으로 생성)
        String merchantUid = "SUB_" + comId + "_" + System.currentTimeMillis();
        Map<String, Object> response = requestrecurrentPayment(
                paymentMethod.getBillingKey(),
                plan.getSubPrice(),
                merchantUid,
                plan.getSubName()
        );

        // 3. 결제 결과 확인 및 구독 상태 업데이트
        Integer code = (Integer) response.get("code");
        if (code == 0) { // 결제 성공
            // 구독 상태 업데이트 (한 달 뒤 결제 예정)
            companySub.renew(LocalDateTime.now().plusMonths(1));
            // 요금제 정보 업데이트
            companySub.updatePlan(plan); // 엔티티에 메서드 추가 필요

            // 결제 내역 저장
            paymentHistoryRepository.save(new PaymentHistory(companySub.getCompany(), plan.getSubPrice(), true, paymentMethod));
        } else {
            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }


    // 포트원 정기 결제 API 호출
    public Map<String, Object> requestrecurrentPayment(String customerUid, BigDecimal amount, String merchantUid, String itemName) {
        String accessToken = getPortoneAccessToken(); // 기존 토큰 발급 메서드 사용

        // 포트원 비인증(빌링키) 결제 API: /subscribe/payments/again
        return webClient.post()
                .uri("/subscribe/payments/again")
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of(
                        "customer_uid", customerUid,
                        "merchant_uid", merchantUid, // 매 결제마다 고유해야 함
                        "amount", amount,
                        "name", itemName
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    /**
     * 구독 해지 예약 (자동 갱신 취소)
     */
    public void cancelSubscription(String comId) {
        // 1. 해당 회사의 구독 정보 조회
        CompanySubscription companySub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 2. 이미 FREE 상태이거나 이미 취소된 경우 체크
        if (companySub.getStatus() == SubStatus.FREE) {
            throw new CustomException(ErrorCode.ALREADY_FREE_PLAN); // "이미 무료 요금제 사용 중입니다"
        }

        if (!companySub.isAutoRenewal() || companySub.getStatus() == SubStatus.CANCELED) {
            throw new CustomException(ErrorCode.ALREADY_CANCELED_SUBSCRIPTION); // "이미 해지 예약된 상태입니다"
        }

        // 3. 엔티티의 해지 로직 호출 (autoRenewal = false, status = CANCELED)
        companySub.cancelSubscription();

        log.info("[구독 해지 예약 완료] 회사: {}, 만료 예정일: {}", comId, companySub.getNextBillingDate());
    }

}