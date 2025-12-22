package com.multi.mlpenterpriseapprovalsystem.common.api.nts.service;

import com.multi.mlpenterpriseapprovalsystem.common.api.nts.dto.NtsStatusDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * 사업자등록번호 검증 서비스 (외부 api 통신)
 * 
 * @filename    : BusinessVerificationService
 * @author      : 권지영
 * @since       : 2025. 12. 19. 금요일
 */
@Service
@Slf4j
public class BusinessVerificationService {

    @Value("${nts.api.service-key}")
    private String serviceKey;

    private final String API_URL = "https://api.odcloud.kr/api/nts-businessman/v1/status?serviceKey=";

    /**
     * 사업자등록번호 존재 여부 확인
     * @param brn 사업자등록번호
     * @return 등록된 번호라면 true, 아니면 false
     */
    public boolean isRegisteredBusiness(String brn) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 하이픈 제거 (숫자 10자리로 변환)
//        String cleanBrn = brn.replaceAll("-", "");

        // 2. 요청 바디 구성
        NtsStatusDto.Request request = new NtsStatusDto.Request(Collections.singletonList(brn));

        try {
            // 3. 국세청 API 호출 (POST)
            NtsStatusDto.Response response = restTemplate.postForObject(
                    API_URL + serviceKey,
                    request,
                    NtsStatusDto.Response.class
            );

            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                String taxType = response.getData().get(0).getTax_type();
                log.info("사업자 번호 [{}] 조회 결과: {}", brn, taxType);

                // "등록되지 않은" 이라는 문구가 포함되어 있지 않으면 유효한 번호로 간주
                return !taxType.contains("등록되지 않은");
            }
        } catch (Exception e) {
            log.error("국세청 API 호출 실패: {}", e.getMessage());
        }

        return false;
    }
}
