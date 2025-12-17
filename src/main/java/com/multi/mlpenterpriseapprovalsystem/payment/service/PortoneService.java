package com.multi.mlpenterpriseapprovalsystem.payment.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.ResPortonePaymentInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

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
}