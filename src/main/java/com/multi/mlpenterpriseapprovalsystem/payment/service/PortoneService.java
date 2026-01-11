package com.multi.mlpenterpriseapprovalsystem.payment.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentHistory;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.res.ResPortonePaymentInfoDto;
import com.multi.mlpenterpriseapprovalsystem.payment.repository.PaymentHistoryRepository;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.CompanySubscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.dto.response.ResSubscriptionResultDto;
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
import java.math.RoundingMode;
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


//    public void subscribeProPlan(String comId, Long subNo) {
//        CompanySubscription sub = companySubscriptionRepository.findByCompany_ComId(comId)
//                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
//        Subscription targetPlan = subscriptionRepository.findById(subNo)
//                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));
//
//        // 1. 중복 요청 체크
//        if (sub.getPendingSubscription() != null && sub.getPendingSubscription().getSubNo().equals(targetPlan.getSubNo())) {
//            throw new CustomException(ErrorCode.ALREADY_PENDING_PLAN);
//        }
//
//        // 2. [핵심] 계층(Level) 기반 업그레이드 판단
//        int currentLevel = getPlanLevel(sub.getSubscription().getSubNo());
//        int targetLevel = getPlanLevel(targetPlan.getSubNo());
//
//        // 타겟 레벨이 현재보다 낮으면 -> 다운그레이드 (예약 처리)
//        if (targetLevel < currentLevel) {
//            log.info("[다운그레이드 예약] {} (L{}) -> {} (L{})",
//                    sub.getSubscription().getSubName(), currentLevel, targetPlan.getSubName(), targetLevel);
//            sub.reservePlanChange(targetPlan);
//            return;
//        }
//
//        // --- 여기서부터는 즉시 업그레이드(전환) 로직 ---
//
//        // 3. 기존 요금제의 잔여 가치를 예치금으로 전환
//        if (sub.getNextBillingDate() != null && sub.getStatus() != SubStatus.FREE) {
//            long remainingDays = java.time.Duration.between(LocalDateTime.now(), sub.getNextBillingDate()).toDays();
//            remainingDays = Math.max(0, remainingDays);
//
//            if (remainingDays > 0) {
//                // 1일당 가치 계산 (소수점 2자리까지 유지하여 계산 정확도 확보)
//                BigDecimal dayValue = sub.getSubscription().getSubPrice().divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
//
//                // [수정 포인트] 최종 적립 금액 계산 시 정수로 반올림(setScale(0)) 처리
//                BigDecimal remainingValue = dayValue.multiply(new BigDecimal(remainingDays))
//                        .setScale(0, RoundingMode.HALF_UP);
//
//                sub.addCredit(remainingValue);
//                log.info("[예치금 적립] 소수점 제거된 정수 금액: {}", remainingValue);
//            }
//        }
//
//        // 4. 새 요금제 결제액 계산 (새 가격 - 현재 보유 예치금)
//        BigDecimal targetPrice = targetPlan.getSubPrice();
//        BigDecimal amountToPay = targetPrice.subtract(sub.getCreditBalance()).max(BigDecimal.ZERO);
//
//        // 5. 예치금 차감 (새 가격만큼 소진)
//        sub.useCredit(targetPrice);
//
//        // 6. 실행 (결제 또는 예치금 100% 처리)
//        if (amountToPay.compareTo(BigDecimal.ZERO) == 0) {
//            log.info("[즉시 업그레이드] 예치금 전액 처리 완료");
//            sub.updatePlan(targetPlan);
//            sub.renew(LocalDateTime.now().plusMonths(1));
//            sub.clearPendingPlan();
//        } else {
//            PaymentMethod paymentMethod = sub.getPaymentMethod();
//            String merchantUid = "UPGRADE_" + comId + "_" + System.currentTimeMillis();
//            Map<String, Object> response = requestrecurrentPayment(paymentMethod.getBillingKey(), amountToPay, merchantUid, targetPlan.getSubName());
//
//            if ((Integer) response.get("code") == 0) {
//                sub.updatePlan(targetPlan);
//                sub.renew(LocalDateTime.now().plusMonths(1));
//                sub.clearPendingPlan();
//                paymentHistoryRepository.save(new PaymentHistory(sub.getCompany(), amountToPay, true, paymentMethod));
//            } else {
//                throw new CustomException(ErrorCode.PAYMENT_FAILED);
//            }
//        }
//    }

//    public BigDecimal subscribeProPlan(String comId, Long subNo) {
//        CompanySubscription sub = companySubscriptionRepository.findByCompany_ComId(comId)
//                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
//        Subscription targetPlan = subscriptionRepository.findById(subNo)
//                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));
//
//        // 1. 중복 요청 체크
//        if (sub.getPendingSubscription() != null && sub.getPendingSubscription().getSubNo().equals(targetPlan.getSubNo())) {
//            throw new CustomException(ErrorCode.ALREADY_PENDING_PLAN);
//        }
//
//        // 2. [핵심] 계층(Level) 기반 업그레이드 판단
//        int currentLevel = getPlanLevel(sub.getSubscription().getSubNo());
//        int targetLevel = getPlanLevel(targetPlan.getSubNo());
//
//        // 타겟 레벨이 현재보다 낮으면 -> 다운그레이드 (예약 처리)
//        if (targetLevel < currentLevel) {
//            log.info("[다운그레이드 예약] {} (L{}) -> {} (L{})",
//                    sub.getSubscription().getSubName(), currentLevel, targetPlan.getSubName(), targetLevel);
//            sub.reservePlanChange(targetPlan);
//            return BigDecimal.ZERO; // 예약 시 결제 금액 0원 반환
//        }
//
//        // --- 여기서부터는 즉시 업그레이드(전환) 로직 ---
//
//        // 3. 기존 요금제의 잔여 가치를 예치금으로 전환
//        if (sub.getNextBillingDate() != null && sub.getStatus() != SubStatus.FREE) {
//            long remainingDays = java.time.Duration.between(LocalDateTime.now(), sub.getNextBillingDate()).toDays();
//            remainingDays = Math.max(0, remainingDays);
//
//            if (remainingDays > 0) {
//                // 1일당 가치 계산 (소수점 2자리까지 유지하여 계산 정확도 확보)
//                BigDecimal dayValue = sub.getSubscription().getSubPrice().divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
//
//                // [수정 포인트] 최종 적립 금액 계산 시 정수로 반올림(setScale(0)) 처리
//                BigDecimal remainingValue = dayValue.multiply(new BigDecimal(remainingDays))
//                        .setScale(0, RoundingMode.HALF_UP);
//
//                sub.addCredit(remainingValue);
//                log.info("[예치금 적립] 소수점 제거된 정수 금액: {}", remainingValue);
//            }
//        }
//
//        // 4. 새 요금제 결제액 계산 (새 가격 - 현재 보유 예치금)
//        BigDecimal targetPrice = targetPlan.getSubPrice();
//        BigDecimal amountToPay = targetPrice.subtract(sub.getCreditBalance()).max(BigDecimal.ZERO);
//
//        // 5. 예치금 차감 (새 가격만큼 소진)
//        sub.useCredit(targetPrice);
//
//        // 6. 실행 (결제 또는 예치금 100% 처리)
//        if (amountToPay.compareTo(BigDecimal.ZERO) == 0) {
//            log.info("[즉시 업그레이드] 예치금 전액 처리 완료");
//            sub.updatePlan(targetPlan);
//            sub.renew(LocalDateTime.now().plusMonths(1));
//            sub.clearPendingPlan();
//            return BigDecimal.ZERO; // 예치금 전액 처리 시 결제 금액 0원 반환
//        } else {
//            PaymentMethod paymentMethod = sub.getPaymentMethod();
//            String merchantUid = "UPGRADE_" + comId + "_" + System.currentTimeMillis();
//            Map<String, Object> response = requestrecurrentPayment(paymentMethod.getBillingKey(), amountToPay, merchantUid, targetPlan.getSubName());
//
//            if ((Integer) response.get("code") == 0) {
//                sub.updatePlan(targetPlan);
//                sub.renew(LocalDateTime.now().plusMonths(1));
//                sub.clearPendingPlan();
//                paymentHistoryRepository.save(new PaymentHistory(sub.getCompany(), amountToPay, true, paymentMethod));
//                return amountToPay; // 실제 결제 성공한 금액 반환
//            } else {
//                throw new CustomException(ErrorCode.PAYMENT_FAILED);
//            }
//        }
//    }

//    public ResSubscriptionResultDto subscribeProPlan(String comId, Long subNo) {
//        CompanySubscription sub = companySubscriptionRepository.findByCompany_ComId(comId)
//                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
//        Subscription targetPlan = subscriptionRepository.findById(subNo)
//                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));
//
//        // 1. 중복 요청 체크
//        if (sub.getPendingSubscription() != null && sub.getPendingSubscription().getSubNo().equals(targetPlan.getSubNo())) {
//            throw new CustomException(ErrorCode.ALREADY_PENDING_PLAN);
//        }
//
//        // 2. [핵심] 계층(Level) 기반 업그레이드 판단
//        int currentLevel = getPlanLevel(sub.getSubscription().getSubNo());
//        int targetLevel = getPlanLevel(targetPlan.getSubNo());
//
//        // 타겟 레벨이 현재보다 낮으면 -> 다운그레이드 (예약 처리)
//        if (targetLevel < currentLevel) {
//            log.info("[다운그레이드 예약] {} (L{}) -> {} (L{})",
//                    sub.getSubscription().getSubName(), currentLevel, targetPlan.getSubName(), targetLevel);
//            sub.reservePlanChange(targetPlan);
//
//            return ResSubscriptionResultDto.builder()
//                    .paidAmount(BigDecimal.ZERO)
//                    .status("RESERVATION_COMPLETED")
//                    .build();
//        }
//
//        // --- 여기서부터는 즉시 업그레이드(전환) 로직 ---
//
//        // 3. 기존 요금제의 잔여 가치를 예치금으로 전환
//        if (sub.getNextBillingDate() != null && sub.getStatus() != SubStatus.FREE) {
//            long remainingDays = java.time.Duration.between(LocalDateTime.now(), sub.getNextBillingDate()).toDays();
//            remainingDays = Math.max(0, remainingDays);
//
//            if (remainingDays > 0) {
//                // 1일당 가치 계산 (소수점 2자리까지 유지하여 계산 정확도 확보)
//                BigDecimal dayValue = sub.getSubscription().getSubPrice().divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);
//
//                // [수정 포인트] 최종 적립 금액 계산 시 정수로 반올림(setScale(0)) 처리
//                BigDecimal remainingValue = dayValue.multiply(new BigDecimal(remainingDays))
//                        .setScale(0, RoundingMode.HALF_UP);
//
//                sub.addCredit(remainingValue);
//                log.info("[예치금 적립] 소수점 제거된 정수 금액: {}", remainingValue);
//            }
//        }
//
//        // 4. 새 요금제 결제액 계산 (새 가격 - 현재 보유 예치금)
//        BigDecimal targetPrice = targetPlan.getSubPrice();
//        BigDecimal amountToPay = targetPrice.subtract(sub.getCreditBalance()).max(BigDecimal.ZERO);
//
//        // 5. 예치금 차감 (새 가격만큼 소진)
//        sub.useCredit(targetPrice);
//
//        // 6. 실행 (결제 또는 예치금 100% 처리)
//        if (amountToPay.compareTo(BigDecimal.ZERO) == 0) {
//            log.info("[즉시 업그레이드] 예치금 전액 처리 완료");
//            sub.updatePlan(targetPlan);
//            sub.renew(LocalDateTime.now().plusMonths(1));
//            sub.clearPendingPlan();
//
//            return ResSubscriptionResultDto.builder()
//                    .paidAmount(BigDecimal.ZERO)
//                    .status("DEPOSIT_ONLY")
//                    .build();
//        } else {
//            PaymentMethod paymentMethod = sub.getPaymentMethod();
//            String merchantUid = "UPGRADE_" + comId + "_" + System.currentTimeMillis();
//            Map<String, Object> response = requestrecurrentPayment(paymentMethod.getBillingKey(), amountToPay, merchantUid, targetPlan.getSubName());
//
//            if ((Integer) response.get("code") == 0) {
//                sub.updatePlan(targetPlan);
//                sub.renew(LocalDateTime.now().plusMonths(1));
//                sub.clearPendingPlan();
//                paymentHistoryRepository.save(new PaymentHistory(sub.getCompany(), amountToPay, true, paymentMethod));
//
//                return ResSubscriptionResultDto.builder()
//                        .paidAmount(amountToPay)
//                        .status("PAYMENT_COMPLETED")
//                        .build();
//            } else {
//                throw new CustomException(ErrorCode.PAYMENT_FAILED);
//            }
//        }
//    }

    public ResSubscriptionResultDto subscribeProPlan(String comId, Long subNo) {
        CompanySubscription sub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
        Subscription targetPlan = subscriptionRepository.findById(subNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

        // 1. 중복 요청 체크
        if (sub.getPendingSubscription() != null && sub.getPendingSubscription().getSubNo().equals(targetPlan.getSubNo())) {
            throw new CustomException(ErrorCode.ALREADY_PENDING_PLAN);
        }

        // 2. [핵심] 계층(Level) 기반 업그레이드 판단
        int currentLevel = getPlanLevel(sub.getSubscription().getSubNo());
        int targetLevel = getPlanLevel(targetPlan.getSubNo());

        // 타겟 레벨이 현재보다 낮으면 -> 다운그레이드 (예약 처리)
        if (targetLevel < currentLevel) {
            log.info("[다운그레이드 예약] {} (L{}) -> {} (L{})",
                    sub.getSubscription().getSubName(), currentLevel, targetPlan.getSubName(), targetLevel);
            sub.reservePlanChange(targetPlan);

            return ResSubscriptionResultDto.builder()
                    .paidAmount(BigDecimal.ZERO)
                    .usedCredit(BigDecimal.ZERO)
                    .status("RESERVATION_COMPLETED")
                    .build();
        }

        // --- 여기서부터는 즉시 업그레이드(전환) 로직 ---

        // 3. 기존 요금제의 잔여 가치를 예치금으로 전환
        if (sub.getNextBillingDate() != null && sub.getStatus() != SubStatus.FREE) {
            long remainingDays = java.time.Duration.between(LocalDateTime.now(), sub.getNextBillingDate()).toDays();
            remainingDays = Math.max(0, remainingDays);

            if (remainingDays > 0) {
                // 1일당 가치 계산 (소수점 2자리까지 유지하여 계산 정확도 확보)
                BigDecimal dayValue = sub.getSubscription().getSubPrice().divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);

                // [수정 포인트] 최종 적립 금액 계산 시 정수로 반올림(setScale(0)) 처리
                BigDecimal remainingValue = dayValue.multiply(new BigDecimal(remainingDays))
                        .setScale(0, RoundingMode.HALF_UP);

                sub.addCredit(remainingValue);
                log.info("[예치금 적립] 소수점 제거된 정수 금액: {}", remainingValue);
            }
        }

        // 4. 새 요금제 결제액 및 예치금 사용액 계산
        BigDecimal targetPrice = targetPlan.getSubPrice();
        BigDecimal currentCredit = sub.getCreditBalance();

        // 실제 결제할 금액: (새 가격 - 보유 예치금)의 결과가 0보다 작으면 0원으로 설정
        BigDecimal amountToPay = targetPrice.subtract(currentCredit).max(BigDecimal.ZERO);

        // 실제 사용된 예치금: 새 가격에서 실제 결제 금액을 뺀 나머지 (즉, 예치금에서 공제된 금액)
        BigDecimal usedCredit = targetPrice.subtract(amountToPay);

        // 5. 예치금 차감 (엔티티 내부 로직 실행)
        sub.useCredit(targetPrice);

        // 6. 실행 (결제 또는 예치금 100% 처리)
        if (amountToPay.compareTo(BigDecimal.ZERO) == 0) {
            log.info("[즉시 업그레이드] 예치금 전액 처리 완료");
            sub.updatePlan(targetPlan);
            sub.renew(LocalDateTime.now().plusMonths(1));
            sub.clearPendingPlan();

            return ResSubscriptionResultDto.builder()
                    .paidAmount(BigDecimal.ZERO)
                    .usedCredit(usedCredit)
                    .status("DEPOSIT_ONLY")
                    .build();
        } else {
            // 결제 수단 확인: 결제가 필요한데 결제 수단 정보가 없으면 예외 발생
            PaymentMethod paymentMethod = sub.getPaymentMethod();
            if (paymentMethod == null || paymentMethod.getBillingKey() == null) {
                throw new CustomException(ErrorCode.PAYMENT_METHOD_NOT_FOUND);
            }

            String merchantUid = "UPGRADE_" + comId + "_" + System.currentTimeMillis();
            Map<String, Object> response = requestrecurrentPayment(paymentMethod.getBillingKey(), amountToPay, merchantUid, targetPlan.getSubName());

            if ((Integer) response.get("code") == 0) {
                sub.updatePlan(targetPlan);
                sub.renew(LocalDateTime.now().plusMonths(1));
                sub.clearPendingPlan();
                paymentHistoryRepository.save(new PaymentHistory(sub.getCompany(), amountToPay, true, paymentMethod));

                return ResSubscriptionResultDto.builder()
                        .paidAmount(amountToPay)
                        .usedCredit(usedCredit)
                        .status("PAYMENT_COMPLETED")
                        .build();
            } else {
                throw new CustomException(ErrorCode.PAYMENT_FAILED);
            }
        }
    }

    /**
     * 요금제 계층(Level) 매핑 헬퍼 메서드
     */
    private int getPlanLevel(Integer subNo) {
        return switch (subNo) {
            case 1 -> 1; // Basic30
            case 2 -> 2; // Pro30
            case 3 -> 3; // Ultimate30
            case 4 -> 4; // Basic50 (인원 확장)
            case 5 -> 5; // Pro50
            case 6 -> 6; // Ultimate50
            case 7 -> 7; // Basic100 (인원 확장)
            case 8 -> 8; // Pro100
            case 9 -> 9; // Ultimate100
            default -> 1;
        };
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
//    public void cancelSubscription(String comId) {
//        // 1. 해당 회사의 구독 정보 조회
//        CompanySubscription companySub = companySubscriptionRepository.findByCompany_ComId(comId)
//                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));
//
//        // 2. 이미 FREE 상태이거나 이미 취소된 경우 체크
//        if (companySub.getStatus() == SubStatus.FREE) {
//            throw new CustomException(ErrorCode.ALREADY_FREE_PLAN); // "이미 무료 요금제 사용 중입니다"
//        }
//
//        if (!companySub.isAutoRenewal() || companySub.getStatus() == SubStatus.CANCELED) {
//            throw new CustomException(ErrorCode.ALREADY_CANCELED_SUBSCRIPTION); // "이미 해지 예약된 상태입니다"
//        }
//
//        // 3. 엔티티의 해지 로직 호출 (autoRenewal = false, status = CANCELED)
//        companySub.cancelSubscription();
//
//        log.info("[구독 해지 예약 완료] 회사: {}, 만료 예정일: {}", comId, companySub.getNextBillingDate());
//    }
    /**
     * 구독 해지 예약 (자동 갱신 취소 및 무료 전환 예약)
     */
    public void cancelSubscription(String comId) {
        // 1. 해당 회사의 구독 정보 조회
        CompanySubscription companySub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 2. 이미 완전한 무료 상태인 경우만 체크
        if (companySub.getStatus() == SubStatus.FREE) {
            throw new CustomException(ErrorCode.ALREADY_FREE_PLAN);
        }

        // 이미 CANCELED 상태이고 예약된 요금제도 없는(이미 무료 전환 확정) 경우에만 에러를 던집니다.
        // 만약 pendingSubscription이 있다면, 그것을 지우고 '순수 해지(무료 전환)'로 변경할 수 있게 합니다.
        if (companySub.getStatus() == SubStatus.CANCELED && companySub.getPendingSubscription() == null) {
            throw new CustomException(ErrorCode.ALREADY_CANCELED_SUBSCRIPTION);
        }

        // 3. 비즈니스 로직 수행
        companySub.clearPendingPlan();   // 예약된 유료 요금제 정보 삭제 (이제 만료 후 무료가 됨)
        companySub.cancelSubscription(); // autoRenewal = false, status = CANCELED 설정

        log.info("[구독 해지 업데이트] 회사: {}, 이제 만료일 이후 무료 요금제로 전환됩니다.", comId);
    }


    /**
     * 구독 유지 (변경 예약 취소 및 자동 갱신 재개)
     */
    public void resumeSubscription(String comId) {
        // 1. 구독 정보 조회
        CompanySubscription sub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 2. 복구 가능 상태 확인 (이미 ACTIVE이면서 예약이 없는 경우 제외)
        if (sub.getStatus() == SubStatus.ACTIVE && sub.getPendingSubscription() == null && sub.isAutoRenewal()) {
            throw new CustomException(ErrorCode.ALREADY_ACTIVE_SUBSCRIPTION); // "이미 활성화된 구독입니다" (에러코드 정의 필요)
        }

        // 3. FREE 상태인 경우 복구 불가 (새로 결제해야 함)
        if (sub.getStatus() == SubStatus.FREE) {
            throw new CustomException(ErrorCode.CANNOT_RESUME_FREE_PLAN); // "만료된 구독은 복구할 수 없습니다"
        }

        // 4. 상태 복구
        sub.resumeSubscription();

        log.info("[구독 유지 확정] 회사: {}, 기존 요금제({})가 유지됩니다.", comId, sub.getSubscription().getSubName());
    }

}