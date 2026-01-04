package com.multi.mlpenterpriseapprovalsystem.payment.service;

import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.payment.domain.PaymentMethod;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.req.ReqVerifyDto;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.res.ResPaymentMethodDto;
import com.multi.mlpenterpriseapprovalsystem.payment.dto.res.ResPortonePaymentInfoDto;
import com.multi.mlpenterpriseapprovalsystem.payment.repository.PaymentCompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.payment.repository.PaymentMethodRepository;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.CompanySubscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.enums.SubStatus;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.CompanySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
    private final CompanySubscriptionRepository companySubscriptionRepository;


    /**
     * 회사의 모든 활성 결제 수단 조회
     */
    @Transactional(readOnly = true)
    public List<ResPaymentMethodDto> getPaymentMethods(String comId) {
        // 회사 및 구독 정보 조회 (대표 카드를 알기 위함)
        CompanySubscription companySub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 현재 설정된 대표 카드의 번호 (없을 수도 있음)
        Long representativePaymNo = (companySub.getPaymentMethod() != null)
                ? companySub.getPaymentMethod().getPaymNo() : null;

        // 해당 회사의 삭제되지 않은(active=true) 모든 카드 조회
        Company company = companySub.getCompany();
        List<PaymentMethod> activeCards = paymentMethodRepository.findAllByCompanyAndActiveTrue(company);

        // DTO 변환 및 대표 카드 여부 설정
        return activeCards.stream()
                .map(card -> new ResPaymentMethodDto(
                        card.getPaymNo(),
                        card.getCardType(),
                        card.getMask(),
                        card.getPaymNo().equals(representativePaymNo) // 대표 카드 여부 확인
                ))
                .collect(Collectors.toList());
    }


    // 카드 등록(프론트에서 받은 빌링키의 유효성 검증 후 카드 등록)
    public void registerCard(String comId, ReqVerifyDto reqVerifyDto) {

        // 회사 조회(존재하는 회사인지 확인)
        Company company = companyRepository.findByComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));

        CompanySubscription companySub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 결제ID로 포트원서버에 정보 조회 요청(실제 데이터 가져오기)
        ResPortonePaymentInfoDto portoneServerPaymentInfo = portoneService.getPaymentInfo(reqVerifyDto.getImpUid());

        // 검증 (프론트에서 보낸 빌링키와 포트원서버가 반환한 실제 발급된 빌링키가 같은지)
        if (portoneServerPaymentInfo.getCustomerUid() == null || !portoneServerPaymentInfo.getCustomerUid().equals(reqVerifyDto.getCustomerUid())) {
            throw new CustomException(ErrorCode.BILLINGKEY_NOT_MATCH);
        }

        String cardNumber = portoneServerPaymentInfo.getCardNumber();
        // 카드 중복 등록 방지 처리
        if(paymentMethodRepository.findByCompanyAndMaskAndActiveTrue(company, cardNumber).isPresent()) {
            // 중복체크 코드 추가
            throw new CustomException(ErrorCode.DUPLICATE_CARD);
        }

        // 엔티티 변환 및 저장
        PaymentMethod newCard = portoneServerPaymentInfo.toEntity(company);
        paymentMethodRepository.save(newCard);

        // 대표 결제 수단 자동 설정 로직
        // 만약 현재 구독 정보에 연결된 카드가 없다면(최초 등록), 방금 등록한 카드를 대표로 설정
        if (companySub.getPaymentMethod() == null) {
            companySub.changePaymentMethod(newCard); // CompanySubscription에 추가한 메서드
            log.info("[대표 카드 자동 설정] 회사: {}, 카드번호: {}", comId, newCard.getMask());
        }

        log.info("카드 등록 완료: comId={}, mask={}", comId, newCard.getMask());
    }


    // 대표결제수단 변경
    public void updateRepresentativeCard(String comId, Long paymNo) {
        // 구독 정보 조회
        CompanySubscription companySub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 변경하려는 카드 정보 조회
        PaymentMethod targetCard = paymentMethodRepository.findByPaymNoAndActiveTrue(paymNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        // 소유권 검증 (해당 카드가 요청한 회사의 것이 맞는지)
        if (!targetCard.getCompany().getComId().equals(comId)) {
            throw new CustomException(ErrorCode.NOT_YOUR_PAYMENT_METHOD);
        }

        // 대표 카드 교체
        companySub.changePaymentMethod(targetCard);
        log.info("[대표 카드 변경 완료] 회사: {}, 카드ID: {}", comId, paymNo);
    }


    // 결제수단 삭제
    public void deleteCard(String comId, Long paymNo) {
        CompanySubscription sub = companySubscriptionRepository.findByCompany_ComId(comId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        PaymentMethod targetCard = paymentMethodRepository.findByPaymNoAndActiveTrue(paymNo)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        // 대표 카드이고 유료 구독 중이면 삭제 불가
        if (sub.getPaymentMethod() != null && sub.getPaymentMethod().getPaymNo().equals(paymNo)) {
            if (sub.getStatus() == SubStatus.ACTIVE) {
                throw new CustomException(ErrorCode.CANNOT_DELETE_REPRESENTATIVE_CARD);
            }
            // 대표 카드지만 삭제 가능한 상태(FREE/CANCELED)라면 연결 해제
            sub.changePaymentMethod(null);
        }

        // 소프트 삭제 처리
        // 실제 DB에서 행을 지우지 않고 상태만 변경합니다.
        targetCard.deactivate(); // PaymentMethod 엔티티에 active = false 메서드 추가 필요
        log.info("[소프트 삭제 완료] 카드ID: {}", paymNo);
    }
}
