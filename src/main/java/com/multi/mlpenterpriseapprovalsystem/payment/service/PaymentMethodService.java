package com.multi.mlpenterpriseapprovalsystem.payment.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.ResPortonePaymentInfoDto;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.ReqVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.payment.repository.PaymentCompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.payment.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 수단 서비스 관리 파일
 * 
 * @filename    : PaymentService
 * @author      : 이지헌
 * @since       : 25. 12. 17. 수요일
 */

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PortoneService portoneService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentCompanyRepository companyRepository;

    // 카드 등록(프론트에서 받은 빌링키의 유효성 검증 후 카드 등록)
    public void registerCard(String comId, ReqVerifyDto reqVerifyDto) {

        // 회사 조회(존재하는 회사인지 확인)
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        // 결제ID로 포트원서버에 정보 조회 요청(실제 데이터 가져오기)
        ResPortonePaymentInfoDto portoneServerPaymentInfo = portoneService.getPaymentInfo(reqVerifyDto.getImpUid());

        // 검증 (프론트에서 보낸 빌링키와 포트원서버가 반환한 실제 발급된 빌링키가 같은지)
        if (portoneServerPaymentInfo.getCustomerUid() == null || !portoneServerPaymentInfo.getCustomerUid().equals(reqVerifyDto.getCustomerUid())) {
            throw new CustomException(ErrorCode.BILLINGKEY_NOT_MATCH);
        }

        String cardNumber = portoneServerPaymentInfo.getCardNumber();
        // 카드 중복 등록 방지 처리
        if(paymentMethodRepository.findByCompanyAndMask(company, cardNumber).isPresent()) {
            // 중복체크 코드 추가
            throw new CustomException(ErrorCode.DUPLICATE_CARD);
        }

        // 엔티티 변환 및 저장
        PaymentMethod paymentMethod = portoneServerPaymentInfo.toEntity(company);
        paymentMethodRepository.save(paymentMethod);

        log.info("카드 등록 완료: comId={}, mask={}", comId, paymentMethod.getMask());
    }
}
